package ru.parking.remoteservice;

import android.os.Looper;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import ru.parking.error.NoNetworkConnectionException;
import ru.parking.error.RemoteServiceError;
import ru.parking.error.RequestTimeoutError;
import timber.log.Timber;

public class EvacuationRemoteServiceImpl extends RemoteServiceImpl {

    private static final String TAG = "EvacuationRemoteTag";

    private final RemoteServiceConfig remoteServiceConfig;
    private final RemoteServiceErrorHandler remoteServiceErrorHandler;
    private final VolleyWrapper volleyWrapper;

    public EvacuationRemoteServiceImpl(RemoteServiceConfig remoteServiceConfig,
                                       RemoteServiceErrorHandler remoteServiceErrorHandler,
                                       VolleyWrapper volleyWrapper) {
        super(remoteServiceConfig, remoteServiceErrorHandler, volleyWrapper);
        this.remoteServiceConfig = remoteServiceConfig;
        this.remoteServiceErrorHandler = remoteServiceErrorHandler;
        this.volleyWrapper = volleyWrapper;
    }

    @Override
    public Observable<JSONObject> getRemoteJson(final int requestMethod,
                                                final String relativePath,
                                                final HashMap<String, Object> params,
                                                final Map<String, String> headers,
                                                final Object sender) {
        return Observable.defer(new Callable<ObservableSource<JSONObject>>() {
            @Override
            public ObservableSource<JSONObject> call() throws Exception {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    throw new WrongThreadException("RemoteService must not be called from the Main thread");
                }
                try {
                    return Observable.just(getRemoteJsonInner(requestMethod,
                            relativePath,
                            params,
                            headers,
                            sender));
                } catch (InterruptedException | ExecutionException error) {
                    Throwable volleyError = error.getCause();
                    Exception serviceError = new Exception("Unknown Remote Service Error");
                    if (volleyError instanceof NoConnectionError) {
                        serviceError = new NoNetworkConnectionException(error.getMessage());
                    } else if (volleyError instanceof TimeoutError) {
                        serviceError = new RequestTimeoutError(error.getMessage());
                    } else {
                        try {
                            JSONObject jsonObject = new JSONObject(error.getCause().getMessage());
                            String userMessage = remoteServiceErrorHandler.getUserMessage(jsonObject);
                            serviceError = new RemoteServiceError(userMessage);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    return Observable.error(serviceError);
                }
            }
        });
    }

    private JSONObject getRemoteJsonInner(final int requestMethod,
                                          final String relativePath,
                                          final HashMap<String, Object> params,
                                          final Map<String, String> headers,
                                          final Object sender) throws ExecutionException, InterruptedException {
        RequestQueue queue = volleyWrapper.getRequestQueue();
        String url = remoteServiceConfig.getBaseUrl();

        String stringBody = getStringBody(params);

        final String fullPath = getFullPath(relativePath, url);
        Timber.tag(TAG).d("Full path: " + fullPath);

        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(requestMethod,
                fullPath,
                stringBody,
                future,
                future) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                    Timber.tag(TAG).d("ParseNetworkResponse: " + json);
                    if (json.isEmpty()) {
                        return ResponseWithoutCacheFactory.get(new JSONObject());
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return super.parseNetworkResponse(response);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> baseHeaders = remoteServiceConfig.getHeaders();
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        baseHeaders.put(entry.getKey(), entry.getValue());
                    }
                }
                Timber.tag(TAG).d("Header for path " + relativePath + ": " + headers);
                if (baseHeaders == null) {
                    baseHeaders = new HashMap<>();
                }
                return baseHeaders;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError) {
                Timber.tag(TAG).d("ParseNetworkError: " + volleyError.getMessage());
                if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                    VolleyError error = new VolleyError(new String(volleyError.networkResponse.data));
                    volleyError = error;
                }
                return volleyError;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded";
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                remoteServiceConfig.getTimeout(),
                remoteServiceConfig.getMaxRetries(),
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        jsonObjectRequest.setTag(sender);
        queue.add(jsonObjectRequest);
        return future.get();
    }


    @Nullable
    private String getStringBody(HashMap<String, Object> params) {
        String stringBody = null;
        if (params != null && !params.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            Iterator<String> keyIterator = params.keySet().iterator();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                builder.append(key.concat("=".concat(params.get(key).toString())));
                if (keyIterator.hasNext()) {
                    builder.append("&");
                }
            }
            stringBody = builder.toString();
        }
        return stringBody;
    }


}