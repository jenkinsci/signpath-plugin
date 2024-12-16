package io.jenkins.plugins.signpath.Common;

/**
 * shared constants for the plugin
 */
public final class PluginConstants {
    private PluginConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static final String DEFAULT_API_URL = "https://app.signpath.io/Api/";
    public static final String DEFAULT_TBS_TOKEN_CREDENTIAL_ID = "SignPath.TrustedBuildSystemToken";
    public static final String DEFAULT_API_TOKEN_CREDENTIAL_ID = "SignPath.ApiToken";
}