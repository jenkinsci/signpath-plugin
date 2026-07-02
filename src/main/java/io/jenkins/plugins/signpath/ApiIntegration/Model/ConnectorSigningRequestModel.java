package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.Map;
import java.util.UUID;

/**
 * All (non-authentication) parameters needed to submit a signing request to the SignPath Pipeline Connector.
 * The connector pulls the artifact and its SHA-256 sidecar from the Jenkins build's archived artifacts
 * (identified by {@code jobFullName} + {@code buildNumber} + {@code sha256ArtifactPath}) and computes the
 * pipeline/origin data itself, so the plugin no longer uploads the artifact or gathers origin metadata.
 */
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

    public ConnectorSigningRequestModel(UUID organizationId,
                                        String jobFullName,
                                        int buildNumber,
                                        String sha256ArtifactPath,
                                        String projectSlug,
                                        String artifactConfigurationSlug,
                                        String signingPolicySlug,
                                        String description,
                                        Map<String, String> parameters,
                                        String inputArtifactRetrievalUrl,
                                        Map<String, String> inputArtifactRetrievalHttpHeaders) {
        this.organizationId = organizationId;
        this.jobFullName = jobFullName;
        this.buildNumber = buildNumber;
        this.sha256ArtifactPath = sha256ArtifactPath;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.description = description;
        this.parameters = parameters;
        this.inputArtifactRetrievalUrl = inputArtifactRetrievalUrl;
        this.inputArtifactRetrievalHttpHeaders = inputArtifactRetrievalHttpHeaders;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getJobFullName() {
        return jobFullName;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public String getSha256ArtifactPath() {
        return sha256ArtifactPath;
    }

    public String getProjectSlug() {
        return projectSlug;
    }

    public String getArtifactConfigurationSlug() {
        return artifactConfigurationSlug;
    }

    public String getSigningPolicySlug() {
        return signingPolicySlug;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getInputArtifactRetrievalUrl() {
        return inputArtifactRetrievalUrl;
    }

    public Map<String, String> getInputArtifactRetrievalHttpHeaders() {
        return inputArtifactRetrievalHttpHeaders;
    }
}
