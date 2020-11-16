package ru.fabit.remoteservice

import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RetrofitApi {

    @GET
    fun getObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Observable<Response<ResponseBody>>

    @PUT
    fun putObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Observable<Response<ResponseBody>>

    @POST
    fun postObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Observable<Response<ResponseBody>>

    @DELETE
    fun deleteObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>
    ): Observable<Response<ResponseBody>>

    @PATCH
    fun patchObject(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Observable<Response<ResponseBody>>
}