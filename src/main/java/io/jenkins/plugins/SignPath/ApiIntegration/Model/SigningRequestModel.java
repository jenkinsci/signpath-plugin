package io.jenkins.plugins.SignPath.ApiIntegration.Model;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.util.UUID;

public class SigningRequestModel {
    private UUID organizationId;
    private String projectSlug;
    private String artifactConfigurationSlug;
    private String signingPolicySlug;
    private String description;
    private SigningRequestOriginModel origin;
    private TemporaryFile artifact;

    public SigningRequestModel(UUID organizationId, String projectSlug, String artifactConfigurationSlug, String signingPolicySlug, String description, SigningRequestOriginModel origin, TemporaryFile artifact){
        this.organizationId = organizationId;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.description = description;
        this.origin = origin;
        this.artifact = artifact;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getProjectSlug() {
        return projectSlug;
    }

    public String getSigningPolicySlug() {
        return signingPolicySlug;
    }

    public String getArtifactConfigurationSlug() {
        return artifactConfigurationSlug;
    }

    public String getDescription() {
        return description;
    }

    public SigningRequestOriginModel getOrigin() {
        return origin;
    }

    public TemporaryFile getArtifact() {
        return artifact;
    }
}
