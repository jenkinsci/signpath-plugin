package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.Map;
import java.util.UUID;

/**
 * Holds all information needed to submit a signing request via the SubmitWithoutArtifact route.
 * The artifact itself is uploaded separately from the agent after this request is submitted.
 */
public class SigningRequestWithoutArtifactModel {
    private final UUID organizationId;
    private final String fileName;
    private final String sha256HexHash;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String description;
    private final SigningRequestOriginModel origin;
    private final Map<String, String> parameters;

    public SigningRequestWithoutArtifactModel(UUID organizationId,
                                              String fileName,
                                              String sha256HexHash,
                                              String projectSlug,
                                              String artifactConfigurationSlug,
                                              String signingPolicySlug,
                                              String description,
                                              SigningRequestOriginModel origin,
                                              Map<String, String> parameters) {
        this.organizationId = organizationId;
        this.fileName = fileName;
        this.sha256HexHash = sha256HexHash;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.description = description;
        this.origin = origin;
        this.parameters = parameters;
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
}
