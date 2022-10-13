package io.jenkins.plugins.signpath.ApiIntegration;

import java.net.URL;

/**
 * Holds all general configuration values that are necessary for talking to the SignPath API
 */
public class ApiConfiguration {
    private final URL apiUrl;
    private final int serviceUnavailableTimeoutInSeconds;
    private final int uploadAndDownloadRequestTimeoutInSeconds;
    private final int waitForCompletionTimeoutInSeconds;
    private final int waitForPowerShellTimeoutInSeconds;
    private final int waitBetwenReadinnesChecksInSeconds;

    public ApiConfiguration(URL apiUrl,
                            int serviceUnavailableTimeoutInSeconds,
                            int uploadAndDownloadRequestTimeoutInSeconds,
                            int waitForCompletionTimeoutInSeconds,
                            int waitForPowerShellTimeoutInSeconds,
                            int waitBetwenReadinnesChecksInSeconds) {
        this.apiUrl = apiUrl;
        this.serviceUnavailableTimeoutInSeconds = serviceUnavailableTimeoutInSeconds;
        this.uploadAndDownloadRequestTimeoutInSeconds = uploadAndDownloadRequestTimeoutInSeconds;
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
        this.waitForPowerShellTimeoutInSeconds = waitForPowerShellTimeoutInSeconds;
        this.waitBetwenReadinnesChecksInSeconds = waitBetwenReadinnesChecksInSeconds;
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
    
    public int getWaitBetwenReadinnesChecksInSeconds() {
        return this.waitBetwenReadinnesChecksInSeconds;
    }
}