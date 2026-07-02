package io.jenkins.plugins.signpath.ApiIntegration;

import java.net.URL;

/**
 * Holds all general configuration values that are necessary for talking to the SignPath Pipeline Connector
 */
public class ApiConfiguration {
    private final URL connectorUrl;
    private final String endpointSlug;
    private final int serviceUnavailableTimeoutInSeconds;
    private final int uploadAndDownloadRequestTimeoutInSeconds;
    private final int waitForCompletionTimeoutInSeconds;
    private final int waitBetweenReadinessChecksInSeconds;

    public ApiConfiguration(URL connectorUrl,
                            String endpointSlug,
                            int serviceUnavailableTimeoutInSeconds,
                            int uploadAndDownloadRequestTimeoutInSeconds,
                            int waitForCompletionTimeoutInSeconds,
                            int waitBetweenReadinessChecksInSeconds) {
        this.connectorUrl = connectorUrl;
        this.endpointSlug = endpointSlug;
        this.serviceUnavailableTimeoutInSeconds = serviceUnavailableTimeoutInSeconds;
        this.uploadAndDownloadRequestTimeoutInSeconds = uploadAndDownloadRequestTimeoutInSeconds;
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
        this.waitBetweenReadinessChecksInSeconds = waitBetweenReadinessChecksInSeconds;
    }

    public URL getConnectorUrl() {
        return connectorUrl;
    }

    public String getEndpointSlug() {
        return endpointSlug;
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

    public int getWaitBetweenReadinessChecksInSeconds() {
        return this.waitBetweenReadinessChecksInSeconds;
    }
}
