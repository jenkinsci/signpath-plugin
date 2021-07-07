package io.jenkins.plugins.signpath.ApiIntegration;

import hudson.util.Secret;

/**
 * Holds all necessary credentials to authenticate against the SignPath API endpoints, exposed by the
 *
 * @see SignPathFacade
 */
public class SignPathCredentials {
    private final Secret ciUserToken;
    private final Secret trustedBuildSystemToken;

    public SignPathCredentials(Secret ciUserToken, Secret trustedBuildSystemToken) {
        this.ciUserToken = ciUserToken;
        this.trustedBuildSystemToken = trustedBuildSystemToken;
    }

    public Secret getCiUserToken() {
        return ciUserToken;
    }

    public Secret getTrustedBuildSystemToken() {
        return trustedBuildSystemToken;
    }

    public Secret toCredentialString() {
        return Secret.fromString(String.format("%s:%s", ciUserToken.getPlainText(), trustedBuildSystemToken.getPlainText()));
    }
}