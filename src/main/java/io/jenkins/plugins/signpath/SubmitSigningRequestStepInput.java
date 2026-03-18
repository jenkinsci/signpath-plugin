package io.jenkins.plugins.signpath;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Holds all input specific to the
 *
 * @see io.jenkins.plugins.signpath.SubmitSigningRequestStep
 */
public class SubmitSigningRequestStepInput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID organizationId;
    private final String trustedBuildSystemTokenCredentialId;
    private final String apiTokenCredentialId;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String inputArtifactPath;
    private final String description;
    private final String outputArtifactPath;
    private final boolean waitForCompletion;
    private final Map<String, String> parameters;
    private final String inputArtifactRetrievalUrl;
    private final Map<String, String> inputArtifactRetrievalHttpHeaders;

    public SubmitSigningRequestStepInput(UUID organizationId,
                                         String trustedBuildSystemTokenCredentialId,
                                         String apiTokenCredentialId,
                                         String projectSlug,
                                         String artifactConfigurationSlug,
                                         String signingPolicySlug,
                                         String inputArtifactPath,
                                         String description,
                                         String outputArtifactPath,
                                         Map<String, String> parameters,
                                         boolean waitForCompletion,
                                         String inputArtifactRetrievalUrl,
                                         Map<String, String> inputArtifactRetrievalHttpHeaders) {
        this.organizationId = organizationId;
        this.trustedBuildSystemTokenCredentialId = trustedBuildSystemTokenCredentialId;
        this.apiTokenCredentialId = apiTokenCredentialId;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.inputArtifactPath = inputArtifactPath;
        this.description = description;
        this.outputArtifactPath = outputArtifactPath;
        this.parameters = parameters;
        this.waitForCompletion = waitForCompletion;
        this.inputArtifactRetrievalUrl = inputArtifactRetrievalUrl;
        this.inputArtifactRetrievalHttpHeaders = inputArtifactRetrievalHttpHeaders;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public String getTrustedBuildSystemTokenCredentialId() {
        return trustedBuildSystemTokenCredentialId;
    }

    public String getApiTokenCredentialId() {
        return apiTokenCredentialId;
    }

    public String getInputArtifactPath() {
        return inputArtifactPath;
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

    public String getOutputArtifactPath() {
        return outputArtifactPath;
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

    public boolean hasArtifactRetrievalUrl() {
        return inputArtifactRetrievalUrl != null && !inputArtifactRetrievalUrl.isEmpty();
    }
}
