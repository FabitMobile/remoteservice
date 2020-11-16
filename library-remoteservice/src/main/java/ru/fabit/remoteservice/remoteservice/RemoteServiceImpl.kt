package ru.fabit.remoteservice.remoteservice

import android.os.Looper
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import ru.fabit.error.AuthFailureException
import ru.fabit.error.NoNetworkConnectionException
import ru.fabit.error.RemoteServiceError
import ru.fabit.error.RequestTimeoutError
import ru.fabit.remoteservice.RetrofitApi
import ru.fabit.remoteservice.entities.RequestMethods
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*

class RemoteServiceImpl(
    private val retrofitBuilder: Retrofit.Builder,
    private val remoteServiceConfig: RemoteServiceConfig,
    private val remoteServiceErrorHandler: RemoteServiceErrorHandler
) : RemoteService {
    override fun getRemoteJson(
        requestMethod: Int,
        relativePath: String?,
        params: HashMap<String, Any>?,
        headers: MutableMap<String, String>?,
        sender: Any?
    ): Observable<JSONObject> {
        return Observable.defer {
            if (Looper.getMainLooper() == Looper.myLooper()) throw IllegalThreadStateException()
            val retrofit = retrofitBuilder
                .baseUrl(remoteServiceConfig.baseUrl)
                .build()
            val api = retrofit.create(RetrofitApi::class.java)
            val url = remoteServiceConfig.baseUrl.plus(relativePath)
            when (requestMethod) {
                RequestMethods.GET -> api.getObject(url, headers ?: hashMapOf(), params ?: mapOf())
                RequestMethods.PUT -> api.putObject(
                    url,
                    headers ?: hashMapOf(),
                    getRequestBody(params)
                )
                RequestMethods.POST -> api.postObject(
                    url,
                    headers ?: hashMapOf(),
                    getRequestBody(params)
                )
                RequestMethods.DELETE -> api.deleteObject(
                    url,
                    headers ?: hashMapOf(),
                    params ?: mapOf()
                )
                RequestMethods.PATCH -> api.patchObject(
                    url,
                    headers ?: hashMapOf(),
                    getRequestBody(params)
                )
                else -> Observable.create { it.onComplete() }
            }
                .onErrorResumeNext(this::onError)
                .map { response: Response<ResponseBody>? ->
                    mapResponseToJSONObject(response, relativePath)
                }
        }
    }

    private fun getRequestBody(params: HashMap<String, Any>?): RequestBody {
        val jsonObject = JSONObject(params?.toMap() ?: mapOf<Any, Any>())
        return RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            jsonObject.toString()
        )
    }

    private fun onError(t: Throwable): Observable<Response<ResponseBody>> {
        return when (t) {
            is SocketTimeoutException -> Observable.error(
                RequestTimeoutError(t.message)
            )
            is IllegalThreadStateException -> Observable.error(
                IllegalThreadStateException(t.message)
            )
            is IOException -> Observable.error(
                NoNetworkConnectionException(t.message)
            )
            else -> Observable.error(RuntimeException(t.message))
        }
    }

    private fun mapResponseToJSONObject(
        response: Response<ResponseBody>?,
        relativePath: String?
    ): JSONObject {
        val body = response?.body()
        return response?.code()?.let { code ->
            when (code) {
                in 200..299 -> {
                    body?.string()?.let { json ->
                        JSONObject(json)
                    } ?: JSONObject()
                }
                401 -> {
                    val message = response.errorBody()?.string()
                        ?.let { parseErrorNetworkResponse(it) }?.userMessage
                        ?: response.message() ?: ""
                    val error = AuthFailureException(message, code)
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
                in 400..599 -> {
                    val remoteError =
                        response.errorBody()?.string()?.let { parseErrorNetworkResponse(it) }
                    val error = RemoteServiceError(
                        code,
                        remoteError?.userMessage ?: response.message() ?: "",
                        remoteError?.code,
                        remoteError?.name
                    )
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
                else -> {
                    val error = RuntimeException("Unexpected response $response")
                    remoteServiceErrorHandler.handleError(error, relativePath)
                    throw error
                }
            }
        } ?: throw RuntimeException("Unexpected response $response")
    }

    private fun parseErrorNetworkResponse(json: String): RemoteError? {
        var errorInfo: RemoteError? = null
        try {
            val jsonObject = JSONObject(json)

            errorInfo =
                RemoteError(
                    userMessage = remoteServiceErrorHandler.getUserMessage(jsonObject),
                    code = remoteServiceErrorHandler.getCode(jsonObject),
                    name = remoteServiceErrorHandler.getErrorName(jsonObject)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return errorInfo
    }

    data class RemoteError(
        val userMessage: String? = null,
        val code: String? = null,
        val name: String? = null
    )
}
