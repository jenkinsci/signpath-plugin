package io.jenkins.plugins.signpath.ApiIntegration;

/**
 * Holds all necessary credentials to authenticate against the SignPath API endpoints, exposed by the
 *
 * @see SignPathFacade
 */
public class SignPathCredentials {
    private final String ciUserToken;
    private final String trustedBuildSystemToken;

    public SignPathCredentials(String ciUserToken, String trustedBuildSystemToken) {
        this.ciUserToken = ciUserToken;
        this.trustedBuildSystemToken = trustedBuildSystemToken;
    }

    public String getCiUserToken() {
        return ciUserToken;
    }

    public String getTrustedBuildSystemToken() {
        return trustedBuildSystemToken;
    }

    public String toCredentialString() {
        return String.format("%s:%s", ciUserToken, trustedBuildSystemToken);
    }
}