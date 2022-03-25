package ru.fabit.remoteservice;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class VolleyWrapper {

    private final RequestQueue requestQueue;

    public VolleyWrapper(Context context,
                         RemoteServiceConfig remoteServiceConfig,
                         Boolean isTrustAllCerts) {
        requestQueue = Volley.newRequestQueue(context, new OkHttp3Stack(remoteServiceConfig, isTrustAllCerts));
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue != null) {
            return requestQueue;
        } else {
            throw new IllegalStateException("RequestQueue not initialized");
        }
    }
}
