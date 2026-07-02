package io.jenkins.plugins.signpath.Common;

/**
 * shared constants for the plugin
 */
public final class PluginConstants {
    private PluginConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static final String DEFAULT_API_TOKEN_CREDENTIAL_ID = "SignPath.ApiToken";

    /**
     * The build-system path segment used when calling the SignPath Pipeline Connector routes
     * (e.g. {@code POST {connectorUrl}/Jenkins/{endpointSlug}/{organizationId}/SigningRequests}).
     */
    public static final String BUILD_SYSTEM_TYPE = "Jenkins";
}