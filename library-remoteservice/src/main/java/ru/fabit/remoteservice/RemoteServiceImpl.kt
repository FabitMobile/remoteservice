package ru.fabit.remoteservice

import  android.os.Looper
import com.android.volley.*
import com.android.volley.toolbox.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.reactivex.Observable
import io.reactivex.ObservableSource
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.zip.GZIPInputStream

/**
 * Класс для совершения запросов.
 *
 * @param remoteServiceConfig       - конфиг
 * @param remoteServiceErrorHandler - обработчик ошибок (достает UseMessage)
 */
open class RemoteServiceImpl(
    private val remoteServiceConfig: RemoteServiceConfig,
    private val remoteServiceErrorHandler: RemoteServiceErrorHandler,
    private val volleyWrapper: VolleyWrapper
) : RemoteService {

    override fun getRemoteJson(
        requestMethod: Int,
        relativePath: String,
        params: HashMap<String, Any>,
        headers: Map<String, String>,
        sender: Any
    ): Observable<JSONObject> {
        return Observable.defer(Callable<ObservableSource<JSONObject>> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw WrongThreadException("RemoteService must not be called from the Main thread")
            }
            try {
                return@Callable Observable.just(
                    getRemoteJsonInner(
                        requestMethod,
                        relativePath,
                        params,
                        headers,
                        sender
                    )
                )
            } catch (error: Exception) {
                val serviceError = errorHandling(error, relativePath)
                return@Callable Observable.error(serviceError)
            } finally {
                volleyWrapper.requestQueue.cache.clear()
            }
        })
    }

    override fun getRemoteJsonArray(
        requestMethod: Int,
        relativePath: String,
        params: HashMap<String, Any>,
        headers: MutableMap<String, String>,
        sender: Any
    ): Observable<JSONArray> {
        return Observable.defer(Callable<ObservableSource<JSONArray>> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw WrongThreadException("RemoteService must not be called from the Main thread")
            }
            try {
                return@Callable Observable.just(
                    getRemoteJsonArrayInner(
                        requestMethod,
                        relativePath,
                        params,
                        headers,
                        sender
                    )
                )
            } catch (error: Exception) {
                val serviceError = errorHandling(error, relativePath)
                return@Callable Observable.error(serviceError)
            } finally {
                volleyWrapper.requestQueue.cache.clear()
            }
        })
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun getRemoteJsonArrayInner(
        requestMethod: Int,
        relativePath: String,
        params: HashMap<String, Any>?,
        headers: Map<String, String>?,
        sender: Any
    ): JSONArray {

        val queue = volleyWrapper.requestQueue
        val url = remoteServiceConfig.baseUrl

        val stringBody = getStringBody(params)
        val jsonBody = getJsonBody(stringBody)

        val fullPath = getFullPath(relativePath, url)

        var urlParams = ""
        if (params != null) {
            urlParams = params.toString()
        }
        Timber.tag("RemoteLog").d("Request: $fullPath\n$urlParams")

        val future = RequestFuture.newFuture<JSONArray>()

        val jsonArrayRequest = object : JsonArrayRequest(
            requestMethod,
            fullPath,
            jsonBody,
            future,
            future
        ) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONArray> {
                try {
                    val output = when (isContainsEncodingContent(response.headers)) {
                        true -> parseGzip(response.data)
                        false -> parse(response.data, response.headers)
                    }
                    val jsonArray = when (output.isNotEmpty()) {
                        true -> JSONArray(output)
                        false -> JSONArray()
                    }
                    return ResponseWithoutCacheFactory.get(jsonArray)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                return super.parseNetworkResponse(response)
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders() = getHeaders(remoteServiceConfig, headers)
        }.apply {
            addRetryPolicy(this, sender)
        }
        queue.add(jsonArrayRequest)
        return future.get()
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun getRemoteJsonInner(
        requestMethod: Int,
        relativePath: String,
        params: HashMap<String, Any>?,
        headers: Map<String, String>?,
        sender: Any
    ): JSONObject {

        val queue = volleyWrapper.requestQueue
        val url = remoteServiceConfig.baseUrl

        val stringBody = getStringBody(params)
        val jsonBody = getJsonBody(stringBody)

        val fullPath = getFullPath(relativePath, url)

        var urlParams = ""
        if (params != null) {
            urlParams = params.toString()
        }
        Timber.tag("RemoteLog").d("Request: $fullPath\n$urlParams")

        val future = RequestFuture.newFuture<JSONObject>()

        val jsonObjectRequest = object : JsonObjectRequest(
            requestMethod,
            fullPath,
            jsonBody,
            future,
            future
        ) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                try {
                    val output = when (isContainsEncodingContent(response.headers)) {
                        true -> parseGzip(response.data)
                        false -> parse(response.data, response.headers)
                    }
                    val jsonObject = when (output.isNotEmpty()) {
                        true -> JSONObject(output)
                        false -> JSONObject()
                    }
                    return ResponseWithoutCacheFactory.get(jsonObject)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                return super.parseNetworkResponse(response)
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders() = getHeaders(remoteServiceConfig, headers)

        }.apply {
            addRetryPolicy(this, sender)
        }
        queue.add(jsonObjectRequest)
        return future.get()
    }

    protected fun getFullPath(relativePath: String, url: String): String {
        return url + relativePath
    }

    private fun getStringBody(params: HashMap<String, Any>?): String? {
        val objectMapper = ObjectMapper()
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
        var stringBody: String? = null
        if (params != null) {
            try {
                stringBody = objectMapper.writeValueAsString(params)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return stringBody
    }

    private fun getJsonBody(stringBody: String?): JSONObject? {
        var jsonBody: JSONObject? = null
        if (stringBody != null) {
            try {
                jsonBody = JSONObject(stringBody)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return jsonBody
    }

    private fun errorHandling(throwable: Throwable, relativePath: String): Exception {
        val volleyError = throwable.cause
        var serviceError = Exception("Unknown Remote Service Error")
        when (volleyError) {
            is NoConnectionError ->
                serviceError = NoNetworkConnectionException(throwable.message)
            is TimeoutError ->
                serviceError = RequestTimeoutError(throwable.message)
            is AuthFailureError -> {
                val statusCode: Int? =
                    (volleyError as? VolleyError)?.networkResponse?.statusCode
                val detailMessage =
                    throwable.cause?.message?.let {
                        parseErrorMessage(JSONObject(it)).let { remoterError ->
                            remoterError.userMessage ?: throwable.message
                        }
                    } ?: run {
                        (volleyError as? VolleyError)?.networkResponse?.let {
                            parseErrorNetworkResponse(it)?.let { remoteError ->
                                remoteError.userMessage ?: throwable.message
                            }
                        }
                    }
                serviceError = AuthFailureException(detailMessage, statusCode)
            }
            else -> try {
                var userMessage = ""
                var code = ""
                var errorName = ""

                throwable.cause?.message?.let {
                    Timber.tag("RemoteLog").d("Error: $throwable with json $it")
                    parseErrorMessage(JSONObject(it)).let { remoterError ->
                        userMessage = remoterError.userMessage ?: ""
                        code = remoterError.code ?: ""
                        errorName = remoterError.name ?: ""
                    }
                } ?: run {
                    (volleyError as? VolleyError)?.networkResponse?.let {
                        parseErrorNetworkResponse(it)?.let { remoteError ->
                            userMessage = remoteError.userMessage ?: ""
                            code = remoteError.code ?: ""
                            errorName = remoteError.name ?: ""
                        }
                    }
                }

                val statusCode: Int? =
                    (volleyError as? VolleyError)?.networkResponse?.statusCode
                serviceError = RemoteServiceError(
                    errorCode = statusCode,
                    detailMessage = userMessage,
                    code = code,
                    errorName = errorName
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        remoteServiceErrorHandler.handleError(serviceError, relativePath)

        Timber.tag("RemoteLog").e("volleyError " + volleyError?.message)
        Timber.tag("RemoteLog").e("serviceError " + serviceError.message)

        return serviceError
    }

    private fun parseErrorNetworkResponse(nerworkResponse: NetworkResponse): RemoteError? {
        var errorInfo: RemoteError? = null
        try {
            val json = java.lang.String(
                nerworkResponse.data,
                HttpHeaderParser.parseCharset(nerworkResponse.headers)
            ).toString()
            val jsonObject = JSONObject(json)

            errorInfo = RemoteError(
                userMessage = remoteServiceErrorHandler.getUserMessage(jsonObject),
                code = remoteServiceErrorHandler.getCode(jsonObject),
                name = remoteServiceErrorHandler.getErrorName(jsonObject)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return errorInfo
    }

    private fun getHeaders(
        remoteServiceConfig: RemoteServiceConfig,
        headers: Map<String, String>?
    ): Map<String, String> {
        val baseHeaders = remoteServiceConfig.headers
        if (headers != null) {
            for ((key, value) in headers) {
                baseHeaders[key] = value
            }
        }
        return baseHeaders
    }

    private fun parseErrorMessage(jsonObject: JSONObject) =
        RemoteError(
            userMessage = remoteServiceErrorHandler.getUserMessage(jsonObject),
            code = remoteServiceErrorHandler.getCode(jsonObject),
            name = remoteServiceErrorHandler.getErrorName(jsonObject)
        )

    private fun isContainsEncodingContent(responseHeaders: Map<String, String>): Boolean =
        responseHeaders.containsKey("Content-Encoding")

    private fun parseGzip(responseData: ByteArray): String {
        val gzipStream = GZIPInputStream(ByteArrayInputStream(responseData))
        val reader = InputStreamReader(gzipStream)
        val br = BufferedReader(reader, responseData.size)
        val output = br.use(BufferedReader::readText)
        reader.close()
        br.close()
        gzipStream.close()
        return output
    }

    private fun parse(responseData: ByteArray, responseHeaders: Map<String, String>): String {
        return java.lang.String(
            responseData,
            HttpHeaderParser.parseCharset(responseHeaders)
        ).toString()
    }

    data class RemoteError(
        val userMessage: String? = null,
        val code: String? = null,
        val name: String? = null
    )

    private fun <T> addRetryPolicy(jsonRequest: JsonRequest<T>, sender: Any) {
        with(jsonRequest) {
            retryPolicy = DefaultRetryPolicy(
                remoteServiceConfig.timeout,
                remoteServiceConfig.maxRetries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            tag = sender
        }
    }
}