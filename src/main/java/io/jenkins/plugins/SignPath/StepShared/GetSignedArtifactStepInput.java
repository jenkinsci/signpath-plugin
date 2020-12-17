package io.jenkins.plugins.SignPath.StepShared;

import java.util.UUID;

/**
 * Holds all input data specific to the
 *
 * @see io.jenkins.plugins.SignPath.GetSignedArtifactStep
 */
public class GetSignedArtifactStepInput {
    private final UUID organizationId;
    private final UUID signingRequestId;
    private final String ciUserTokenCredentialId;
    private final String outputArtifactPath;

    public GetSignedArtifactStepInput(UUID organizationId, UUID signingRequestId, String ciUserTokenCredentialId, String outputArtifactPath) {
        this.organizationId = organizationId;
        this.signingRequestId = signingRequestId;
        this.ciUserTokenCredentialId = ciUserTokenCredentialId;
        this.outputArtifactPath = outputArtifactPath;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    public String getCiUserTokenCredentialId() {
        return ciUserTokenCredentialId;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }
}