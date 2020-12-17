package io.jenkins.plugins.SignPath.StepShared;

import java.util.UUID;

/**
 * Holds all input specific to the
 *
 * @see io.jenkins.plugins.SignPath.SubmitSigningRequestStep
 */
public class SubmitSigningRequestStepInput {
    private final UUID organizationId;
    private final String ciUserTokenCredentialId;
    private final String projectSlug;
    private final String artifactConfigurationSlug;
    private final String signingPolicySlug;
    private final String inputArtifactPath;
    private final String description;
    private final String outputArtifactPath;
    private final boolean waitForCompletion;

    public SubmitSigningRequestStepInput(UUID organizationId,
                                         String ciUserTokenCredentialId,
                                         String projectSlug,
                                         String artifactConfigurationSlug,
                                         String signingPolicySlug,
                                         String inputArtifactPath,
                                         String description,
                                         String outputArtifactPath,
                                         boolean waitForCompletion) {
        this.organizationId = organizationId;
        this.ciUserTokenCredentialId = ciUserTokenCredentialId;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.inputArtifactPath = inputArtifactPath;
        this.description = description;
        this.outputArtifactPath = outputArtifactPath;
        this.waitForCompletion = waitForCompletion;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public String getCiUserTokenCredentialId() {
        return ciUserTokenCredentialId;
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
}
