package io.jenkins.plugins.SignPath.StepShared;

import java.util.UUID;

public class SubmitSigningRequestStepInput {
    private UUID organizationId;
    private String ciUserToken;
    private String projectSlug;
    private String artifactConfigurationSlug;
    private String signingPolicySlug;
    private String inputArtifactPath;
    private String description;
    private String outputArtifactPath;
    private Boolean waitForCompletion;

    public SubmitSigningRequestStepInput(UUID organizationId,
                                         String ciUserToken,
                                         String projectSlug,
                                         String artifactConfigurationSlug,
                                         String signingPolicySlug,
                                         String inputArtifactPath,
                                         String description,
                                         String outputArtifactPath,
                                         Boolean waitForCompletion) {
        this.organizationId = organizationId;
        this.ciUserToken = ciUserToken;
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

    public Boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public String getCiUserToken() {
        return ciUserToken;
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
