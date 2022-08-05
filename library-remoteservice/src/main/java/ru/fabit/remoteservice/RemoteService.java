package ru.fabit.remoteservice;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;


public interface RemoteService {
    Observable<JSONObject> getRemoteJson(final int requestMethod,
                                         final String relativePath,
                                         final HashMap<String, Object> params,
                                         final Map<String, String> headers,
                                         final Object sender);

    Observable<JSONArray> getRemoteJsonArray(final int requestMethod,
                                             final String relativePath,
                                             final HashMap<String, Object> params,
                                             final Map<String, String> headers,
                                             final Object sender);
}
