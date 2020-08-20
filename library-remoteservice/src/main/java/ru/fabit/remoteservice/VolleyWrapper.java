package ru.fabit.remoteservice;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class VolleyWrapper {

    private final RequestQueue requestQueue;

    public VolleyWrapper(Context context, RemoteServiceConfig remoteServiceConfig) {
        requestQueue = Volley.newRequestQueue(context, new OkHttp3Stack(remoteServiceConfig));
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue != null) {
            return requestQueue;
        } else {
            throw new IllegalStateException("RequestQueue not initialized");
        }
    }
}
