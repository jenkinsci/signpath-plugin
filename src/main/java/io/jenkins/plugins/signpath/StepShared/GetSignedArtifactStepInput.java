package io.jenkins.plugins.signpath.StepShared;

import java.util.UUID;

/**
 * Holds all input data specific to the
 *
 * @see io.jenkins.plugins.signpath.GetSignedArtifactStep
 */
public class GetSignedArtifactStepInput {
    private final UUID organizationId;
    private final UUID signingRequestId;
    private final String trustedBuildSystemTokenCredentialId;
    private final String ciUserTokenCredentialId;
    private final String outputArtifactPath;

    public GetSignedArtifactStepInput(UUID organizationId, UUID signingRequestId, String trustedBuildSystemTokenCredentialId, String ciUserTokenCredentialId, String outputArtifactPath) {
        this.organizationId = organizationId;
        this.signingRequestId = signingRequestId;
        this.trustedBuildSystemTokenCredentialId = trustedBuildSystemTokenCredentialId;
        this.ciUserTokenCredentialId = ciUserTokenCredentialId;
        this.outputArtifactPath = outputArtifactPath;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    public String getTrustedBuildSystemTokenCredentialId() {
        return trustedBuildSystemTokenCredentialId;
    }

    public String getCiUserTokenCredentialId() {
        return ciUserTokenCredentialId;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }
}