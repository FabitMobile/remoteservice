package ru.fabit.remoteservice;


import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttp3Stack implements HttpStack {

    private final OkHttpClient client;
    private final static int DEFAULT_CACHE_STORAGE_TIME_SECONDS = 60;

    final TrustManager[] trustAllCertsManager = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
    };


    public OkHttp3Stack(Context context, RemoteServiceConfig remoteServiceConfig, Boolean isTrustAllCerts, List<Interceptor> interceptorList) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (isTrustAllCerts) {
            SSLSocketFactory sslSocketFactory = null;
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCertsManager, new java.security.SecureRandom());
                sslSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            if (sslSocketFactory != null) {
                clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCertsManager[0]);
            }
            clientBuilder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        clientBuilder.connectTimeout(remoteServiceConfig.getTimeout(), TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(remoteServiceConfig.getTimeout(), TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(remoteServiceConfig.getTimeout(), TimeUnit.MILLISECONDS);
        clientBuilder.cache(new Cache(new File(context.getCacheDir(), "http-cache"), 10L * 1024L * 1024L));
        clientBuilder.addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR);
        for (Interceptor interceptor : interceptorList) {
            clientBuilder.addInterceptor(interceptor);
        }
        this.client = clientBuilder.build();
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());
        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }
        for (final String name : additionalHeaders.keySet()) {
            okHttpRequestBuilder.addHeader(name, additionalHeaders.get(name));
        }
        setConnectionParametersForRequest(okHttpRequestBuilder, request);

        okhttp3.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = client.newCall(okHttpRequest);
        Response okHttpResponse = okHttpCall.execute();
        StatusLine responseStatus = new BasicStatusLine(parseProtocol(okHttpResponse.protocol()), okHttpResponse.code(), okHttpResponse.message());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromOkHttpResponse(okHttpResponse));
        Headers responseHeaders = okHttpResponse.headers();
        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);
            if (name != null) {
                response.addHeader(new BasicHeader(name, value));
            }
        }
        return response;
    }

    private static HttpEntity entityFromOkHttpResponse(Response r) {
        BasicHttpEntity entity = new BasicHttpEntity();
        ResponseBody body = r.body();
        entity.setContent(body.byteStream());
        entity.setContentLength(body.contentLength());
        entity.setContentEncoding(r.header("Content-Encoding"));
        if (body.contentType() != null) {
            entity.setContentType(body.contentType().type());
        }
        return entity;
    }

    @SuppressWarnings("deprecation")
    private static void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request)
            throws AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    builder.post(RequestBody.create(MediaType.parse(request.getPostBodyContentType()), postBody));
                }
                break;
            case Request.Method.GET:
                builder.get();
                break;
            case Request.Method.DELETE:
                builder.delete(createRequestBody(request));
                break;
            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;
            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;
            case Request.Method.HEAD:
                builder.head();
                break;
            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;
            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static ProtocolVersion parseProtocol(final Protocol p) {
        switch (p) {
            case HTTP_1_0:
                return new ProtocolVersion("HTTP", 1, 0);
            case HTTP_1_1:
                return new ProtocolVersion("HTTP", 1, 1);
            case SPDY_3:
                return new ProtocolVersion("SPDY", 3, 1);
            case HTTP_2:
                return new ProtocolVersion("HTTP", 2, 0);
        }
        throw new IllegalAccessError("Unkwown protocol");
    }

    private static RequestBody createRequestBody(Request r) throws AuthFailureError {
        final byte[] body = r.getBody();
        if (body == null) {
            return null;
        }
        return RequestBody.create(body, MediaType.parse(r.getBodyContentType()));
    }

    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = chain -> {
        okhttp3.Request request = chain.request();
        CacheControl.Builder cacheControlBuilder = new CacheControl.Builder();
        CacheControl cacheControl;
        if (chain.request().header(HeaderName.CACHE_CONTROL_SECONDS) == null) {
            cacheControlBuilder.noCache();
        } else {
            int time = DEFAULT_CACHE_STORAGE_TIME_SECONDS;
            try {
                time = Integer.parseInt(chain.request().header(HeaderName.CACHE_CONTROL_SECONDS));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            cacheControlBuilder.maxStale(time, TimeUnit.SECONDS);
        }
        cacheControl = cacheControlBuilder.build();
        okhttp3.Request cachedRequest = request.newBuilder()
                .cacheControl(cacheControl)
                .build();
        if (chain.request().header(HeaderName.CACHE_CONTROL_SECONDS) == null) {
            return chain.proceed(cachedRequest);
        } else {
            return chain.proceed(cachedRequest).newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .removeHeader("Pragma")
                    .build();
        }
    };
}