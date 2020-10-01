package io.jenkins.plugins.SignPath.ApiIntegration;

public class SignPathCredentials {
    private String ciUserToken;
    private String trustedBuildSystemToken;

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
}
