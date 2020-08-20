package ru.parking.remoteservice;

import org.json.JSONException;
import org.json.JSONObject;


public class DefaultRemoteServiceErrorHandler implements RemoteServiceErrorHandler {

    @Override
    public String getUserMessage(JSONObject jsonObject) {
        String userMessage = "";
        try {
            userMessage = jsonObject.getString("userMessage");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return userMessage;
    }

    @Override
    public String getErrorName(JSONObject jsonObject) {
        String errorName = "";
        try {
            errorName = jsonObject.getString("errorName");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return errorName;
    }

    @Override
    public String getCode(JSONObject jsonObject) {
        return "";
    }

    @Override
    public void handleError(Throwable throwable, String requestPath) {
    }
}
