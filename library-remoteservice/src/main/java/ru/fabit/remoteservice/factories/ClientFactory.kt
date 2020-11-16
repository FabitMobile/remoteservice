package ru.fabit.remoteservice.factories

import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.*
import ru.fabit.remoteservice.remoteservice.RemoteServiceConfig
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class ClientFactory {

    fun create(
        remoteServiceConfig: RemoteServiceConfig,
        authenticator: Authenticator
    ): OkHttpClient {
        val builder = getPreconfiguredClientBuilder(
            remoteServiceConfig.connectTimeoutMillis,
            remoteServiceConfig.readTimeoutMillis
        )
        addInterceptors(
            builder,
            authenticator,
            Headers.of(remoteServiceConfig.defaultHeaders),
            remoteServiceConfig.isLogEnabled
        )
        return builder.build()
    }

    private fun getPreconfiguredClientBuilder(
        connectTimeoutMillis: Long,
        readTimeoutMillis: Long
    ): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun addInterceptors(
        builder: OkHttpClient.Builder,
        authenticator: Authenticator,
        headers: Headers,
        isLogEnabled: Boolean
    ) {
        with(builder) {
            authenticator(authenticator)
            addInterceptor(getLoggingInterceptor(isLogEnabled))
            addInterceptor(getInterceptors(headers))
            addInterceptor(getAuthorizationHeaderInterceptor(authenticator))
        }
    }

    private fun getInterceptors(headers: Headers) = Interceptor { chain ->
        var request = chain.request()
        val requestBuilder = request.newBuilder()
        val includedHeaders = request.headers()
        val newHeaders = includedHeaders.newBuilder()
        for (key in headers.names()) {
            if (includedHeaders.get(key) == null) {
                newHeaders.add(key, headers.get(key) ?: "")
            }
        }
        request = requestBuilder
            .headers(newHeaders.build())
            .build()
        chain.proceed(request)
    }


    private fun getLoggingInterceptor(isLogEnabled: Boolean): LoggingInterceptor {
        return LoggingInterceptor.Builder()
            .loggable(isLogEnabled)
            .setLevel(Level.BASIC)
            .request("Request")
            .response("Response")
            .build()
    }

    private fun getAuthorizationHeaderInterceptor(authenticator: Authenticator) =
        Interceptor { chain ->
            if (isRequestWithAccessToken(chain.request())) {
                chain.proceed(
                    authenticator.authenticate(
                        null,
                        Response.Builder()
                            .request(chain.request().newBuilder().build())
                            .protocol(Protocol.HTTP_2)
                            .code(HttpURLConnection.HTTP_PROXY_AUTH)
                            .message("")
                            .build()
                    )
                )
            } else {
                chain.proceed(chain.request().newBuilder().build())
            }
        }

    private fun isRequestWithAccessToken(request: Request): Boolean {
        val header = request.header("Authorization")
        return header != null && header.startsWith("Bearer")
    }

}