package io.jenkins.plugins.signpath.Common;

/**
 * shared constants for the plugin
 */
public final class PluginConstants {
    private PluginConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static final String DEFAULT_API_URL = "https://app.signpath.io/api/";
    public static final String DEFAULT_TBS_CREDENTIAL_ID = "SignPath.TrustedBuildSystemToken";
}