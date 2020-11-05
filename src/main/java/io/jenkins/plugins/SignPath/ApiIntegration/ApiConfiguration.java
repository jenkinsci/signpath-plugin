package io.jenkins.plugins.SignPath.ApiIntegration;

import java.net.URL;

/**
 * Holds all general configuration values that are necessary for talking to the SignPath Api
 */
public class ApiConfiguration {
    private final URL apiUrl;
    private final int serviceUnavailableTimeoutInSeconds;
    private final int uploadAndDownloadRequestTimeoutInSeconds;
    private final int waitForCompletionTimeoutInSeconds;
    private final int waitForPowerShellTimeoutInSeconds;

    public ApiConfiguration(URL apiUrl, int serviceUnavailableTimeoutInSeconds, int uploadAndDownloadRequestTimeoutInSeconds, int waitForCompletionTimeoutInSeconds, int waitForPowerShellTimeoutInSeconds) {
        this.apiUrl = apiUrl;
        this.serviceUnavailableTimeoutInSeconds = serviceUnavailableTimeoutInSeconds;
        this.uploadAndDownloadRequestTimeoutInSeconds = uploadAndDownloadRequestTimeoutInSeconds;
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
        this.waitForPowerShellTimeoutInSeconds = waitForPowerShellTimeoutInSeconds;
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

    public int getWaitForPowerShellTimeoutInSeconds() {
        return waitForPowerShellTimeoutInSeconds;
    }
}