package ru.fabit.remoteservice;

import org.json.JSONObject;


public interface RemoteServiceRequestCallback {
    void onCompletion(JSONObject remoteServiceRequest, int httpStatus);

    void onError(Error remoteServiceError);
}
