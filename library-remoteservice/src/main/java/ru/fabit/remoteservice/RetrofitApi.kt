package ru.fabit.remoteservice

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RetrofitApi {

    @GET
    fun getObject(
        @Url url: String,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>,
        @HeaderMap headers: Map<String, String>
    ): Observable<Response<ResponseBody>>

    @PUT
    fun putObject(
        @Url url: String,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>,
        @HeaderMap headers: Map<String, String>
    ): Observable<Response<ResponseBody>>

    @POST
    fun postObject(
        @Url url: String,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>,
        @HeaderMap headers: Map<String, String>
    ): Observable<Response<ResponseBody>>

    @DELETE
    fun deleteObject(
        @Url url: String,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>,
        @HeaderMap headers: Map<String, String>
    ): Observable<Response<ResponseBody>>

    @PATCH
    fun patchObject(
        @Url url: String,
        @QueryMap queries: Map<String, @JvmSuppressWildcards Any>,
        @HeaderMap headers: Map<String, String>
    ): Observable<Response<ResponseBody>>
}