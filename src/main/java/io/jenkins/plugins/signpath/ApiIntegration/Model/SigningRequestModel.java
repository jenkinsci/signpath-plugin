package io.jenkins.plugins.signpath.ApiIntegration.Model;

import io.jenkins.plugins.signpath.Common.TemporaryFile;
import java.util.Map;

import java.util.UUID;

/**
 * This class holds all the information that is needed to create a signing request on SignPath
 */
public class SigningRequestModel {
    private final UUID organizationId;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String description;
    private final SigningRequestOriginModel origin;
    private final TemporaryFile artifact;
    private final Map<String, String> parameters;

    public SigningRequestModel(UUID organizationId, String projectSlug, String artifactConfigurationSlug,
            String signingPolicySlug, String description, SigningRequestOriginModel origin,
            TemporaryFile artifact, Map<String, String> parameters) {
        this.organizationId = organizationId;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.description = description;
        this.origin = origin;
        this.artifact = artifact;
        this.parameters = parameters;
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
    
    public Map<String, String> getParameters() {
        return parameters;
    }
}
