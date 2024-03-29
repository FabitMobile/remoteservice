package ru.fabit.remoteservice;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.List;

import okhttp3.Interceptor;

public class VolleyWrapper {

    private final RequestQueue requestQueue;

    public VolleyWrapper(
            Context context,
            RemoteServiceConfig remoteServiceConfig,
            Boolean isTrustAllCerts,
            List<Interceptor> interceptorList
    ) {
        requestQueue = Volley.newRequestQueue(context, new OkHttp3Stack(context, remoteServiceConfig, isTrustAllCerts, interceptorList));
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue != null) {
            return requestQueue;
        } else {
            throw new IllegalStateException("RequestQueue not initialized");
        }
    }
}
