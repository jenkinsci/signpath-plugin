package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.Map;
import java.util.UUID;

/**
 * Holds all information needed to submit a signing request via the SubmitWithArtifactRetrievalLink route.
 * SignPath will download the artifact directly from the provided URL instead of receiving an upload.
 */
public class SigningRequestWithArtifactRetrievalLinkModel {
    private final UUID organizationId;
    private final String fileName;
    private final String sha256HexHash;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String description;
    private final SigningRequestOriginModel origin;
    private final Map<String, String> parameters;
    private final String retrievalUrl;
    private final Map<String, String> retrievalHttpHeaders;

    public SigningRequestWithArtifactRetrievalLinkModel(UUID organizationId,
                                                        String fileName,
                                                        String sha256HexHash,
                                                        String projectSlug,
                                                        String artifactConfigurationSlug,
                                                        String signingPolicySlug,
                                                        String description,
                                                        SigningRequestOriginModel origin,
                                                        Map<String, String> parameters,
                                                        String retrievalUrl,
                                                        Map<String, String> retrievalHttpHeaders) {
        this.organizationId = organizationId;
        this.fileName = fileName;
        this.sha256HexHash = sha256HexHash;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.description = description;
        this.origin = origin;
        this.parameters = parameters;
        this.retrievalUrl = retrievalUrl;
        this.retrievalHttpHeaders = retrievalHttpHeaders;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha256HexHash() {
        return sha256HexHash;
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

    public SigningRequestOriginModel getOrigin() {
        return origin;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getRetrievalUrl() {
        return retrievalUrl;
    }

    public Map<String, String> getRetrievalHttpHeaders() {
        return retrievalHttpHeaders;
    }
}
