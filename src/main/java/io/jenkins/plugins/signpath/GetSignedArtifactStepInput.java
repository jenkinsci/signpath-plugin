package io.jenkins.plugins.signpath;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Holds all input data specific to the
 *
 * @see io.jenkins.plugins.signpath.GetSignedArtifactStep
 */
@Getter
@AllArgsConstructor
public class GetSignedArtifactStepInput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID organizationId;
    private final UUID signingRequestId;
    private final String apiTokenCredentialId;
    private final String outputArtifactPath;
}