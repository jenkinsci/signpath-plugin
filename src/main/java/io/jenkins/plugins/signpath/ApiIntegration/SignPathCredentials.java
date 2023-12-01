package io.jenkins.plugins.signpath.ApiIntegration;

import hudson.util.Secret;

/**
 * Holds all necessary credentials to authenticate against the SignPath API endpoints, exposed by the
 *
 * @see SignPathFacade
 */
public class SignPathCredentials {
    private final Secret apiToken;
    private final Secret trustedBuildSystemToken;

    public SignPathCredentials(Secret apiToken, Secret trustedBuildSystemToken) {
        this.apiToken = apiToken;
        this.trustedBuildSystemToken = trustedBuildSystemToken;
    }
    
    public Secret getApiToken() {
        return apiToken;
    }
    
    public Secret getTrustedBuildSystemToken() {
        return trustedBuildSystemToken;
    }

    public Secret toCredentialString() {
        return Secret.fromString(String.format("%s:%s", apiToken.getPlainText(), trustedBuildSystemToken.getPlainText()));
    }
}