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
    private final String ciUserToken;
    private final String outputArtifactPath;

    public GetSignedArtifactStepInput(UUID organizationId, UUID signingRequestId, String ciUserToken, String outputArtifactPath) {
        this.organizationId = organizationId;
        this.signingRequestId = signingRequestId;
        this.ciUserToken = ciUserToken;
        this.outputArtifactPath = outputArtifactPath;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    public String getCiUserToken() {
        return ciUserToken;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }
}