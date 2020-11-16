package ru.fabit.remoteservice.factories

import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit

class RetrofitFactory constructor(
    private val client: OkHttpClient,
    private val adapter: CallAdapter.Factory
) {
    fun getRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
            .client(client)
            .addCallAdapterFactory(adapter)
    }
}
