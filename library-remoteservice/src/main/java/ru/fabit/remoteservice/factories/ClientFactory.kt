package ru.fabit.remoteservice.factories

import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ClientFactory constructor(
    private val locale: String,
    private val authenticator: Authenticator,
    private val isLogEnabled: Boolean
) {

    private val CONNECT_TIMEOUT_MILLIS = 120000L
    private val READ_TIMEOUT_MILLIS = 120000L

    fun create(): OkHttpClient {
        val builder = getPreconfiguredClientBuilder()

        addInterceptors(builder)

        return builder.build()
    }

    private fun getPreconfiguredClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    private fun addInterceptors(builder: OkHttpClient.Builder) {
        with(builder) {
            authenticator(authenticator)
            addInterceptor(getLoggingInterceptor())
            addInterceptor(getLanguageInterceptor())
            addInterceptor(getCharsetInterceptor())
        }
    }

    private fun getLanguageInterceptor() = Interceptor { chain ->
        var request = chain.request()
        request = request.newBuilder()
            .header("Content-Language", locale)
            .build()
        chain.proceed(request)
    }

    private fun getCharsetInterceptor() = Interceptor { chain ->
        var request = chain.request()
        request = request.newBuilder()
            .header("content-type", "application/json; charset=utf-8")
            .build()
        chain.proceed(request)
    }


    private fun getLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor.Builder()
            .loggable(isLogEnabled)
            .setLevel(Level.BASIC)
            .request("Request")
            .response("Response")
            .build()
    }

}