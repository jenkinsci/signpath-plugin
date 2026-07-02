package io.jenkins.plugins.signpath.ApiIntegration.Model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * All (non-authentication) parameters needed to submit a signing request to the SignPath Pipeline Connector.
 * The connector pulls the artifact and its SHA-256 sidecar from the Jenkins build's archived artifacts
 * (identified by {@code jobFullName} + {@code buildNumber} + {@code sha256ArtifactPath}) and computes the
 * pipeline/origin data itself, so the plugin no longer uploads the artifact or gathers origin metadata.
 */
@Getter
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
