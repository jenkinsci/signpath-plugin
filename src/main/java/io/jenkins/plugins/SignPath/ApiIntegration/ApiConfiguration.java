package io.jenkins.plugins.SignPath.ApiIntegration;

import java.net.URL;

public class ApiConfiguration {
    private URL apiUrl;
    private int serviceUnavailableTimeoutInSeconds;
    private int uploadAndDownloadRequestTimeoutInSeconds;
    private int waitForCompletionTimeoutInSeconds;

    public ApiConfiguration(URL apiUrl, int serviceUnavailableTimeoutInSeconds, int uploadAndDownloadRequestTimeoutInSeconds, int waitForCompletionTimeoutInSeconds) {
        this.apiUrl = apiUrl;
        this.serviceUnavailableTimeoutInSeconds = serviceUnavailableTimeoutInSeconds;
        this.uploadAndDownloadRequestTimeoutInSeconds = uploadAndDownloadRequestTimeoutInSeconds;
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
    }

    public URL getApiUrl() {
        return apiUrl;
    }

    public int getServiceUnavailableTimeoutInSeconds() {
        return serviceUnavailableTimeoutInSeconds;
    }

    public int getUploadAndDownloadRequestTimeoutInSeconds() {
        return uploadAndDownloadRequestTimeoutInSeconds;
    }

    public int getWaitForCompletionTimeoutInSeconds() {
        return waitForCompletionTimeoutInSeconds;
    }
}
