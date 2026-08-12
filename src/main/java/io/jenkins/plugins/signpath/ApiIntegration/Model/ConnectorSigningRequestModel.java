package io.jenkins.plugins.signpath.ApiIntegration.Model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

/**
 * All (non-authentication) parameters needed to submit a signing request to the SignPath Pipeline Connector.
 * The connector pulls the SHA-256 sidecar from the Jenkins build's archived artifacts
 * (identified by {@code jobFullName} + {@code buildNumber} + {@code sha256ArtifactPath}) and computes the
 * pipeline/origin data itself, so the plugin no longer gathers origin metadata. The unsigned artifact itself
 * is uploaded in a separate request through the connector.
 */
@Getter
@Setter
@Builder
public class ConnectorSigningRequestModel {
    private final UUID organizationId;
    private final String jobFullName;
    private final int buildNumber;
    private final String sha256ArtifactPath;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String description;
    private final Map<String, String> parameters;
    private final String inputArtifactRetrievalUrl;
    private final Map<String, String> inputArtifactRetrievalHttpHeaders;
}
