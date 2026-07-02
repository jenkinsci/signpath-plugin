package io.jenkins.plugins.signpath.ApiIntegration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URL;

/**
 * Holds all general configuration values that are necessary for talking to the SignPath Pipeline Connector
 */
@Getter
@AllArgsConstructor
public class ApiConfiguration {
    private final URL connectorUrl;
    private final String endpointSlug;
    private final int serviceUnavailableTimeoutInSeconds;
    private final int uploadAndDownloadRequestTimeoutInSeconds;
    private final int waitForCompletionTimeoutInSeconds;
    private final int waitBetweenReadinessChecksInSeconds;
}
