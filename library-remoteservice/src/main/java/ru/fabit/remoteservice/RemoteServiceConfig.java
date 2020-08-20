package ru.fabit.remoteservice;

import java.util.Map;


public interface RemoteServiceConfig {

    String getBaseUrl();

    String getUploadServerUrl();

    Map<String, String> getHeaders();

    int getTimeout();

    int getMaxRetries();

}
