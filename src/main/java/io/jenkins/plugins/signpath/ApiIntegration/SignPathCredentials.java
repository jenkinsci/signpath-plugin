package io.jenkins.plugins.signpath.ApiIntegration;

import hudson.util.Secret;

/**
 * Holds all necessary credentials to authenticate against the SignPath Pipeline Connector endpoints, exposed by the
 *
 * @see SignPathFacade
 */
public class SignPathCredentials {
    private final Secret apiToken;

    public SignPathCredentials(Secret apiToken) {
        this.apiToken = apiToken;
    }

    public Secret getApiToken() {
        return apiToken;
    }
}
