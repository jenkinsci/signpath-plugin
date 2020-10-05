package io.jenkins.plugins.SignPath.StepInputParser;

import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.SubmitSigningRequestStep;

import java.util.UUID;

public class SigningRequestStepInputParser {

    public static SigningRequestStepInput Parse(SubmitSigningRequestStep step) throws SignPathStepInvalidArgumentException {
        return new SigningRequestStepInput(
                ensureValidUUID(step.getOrganizationId(), "organizationId"),
                ensureNotNull(step.getCiUserToken(), "ciUserToken"),
                ensureNotNull(step.getProjectSlug(), "projectSlug"),
                ensureNotNull(step.getArtifactConfigurationSlug(), "artifactConfigurationSlug"),
                ensureNotNull(step.getSigningPolicySlug(), "signingPolicySlug"),
                ensureNotNull(step.getInputArtifactPath(), "inputArtifactPath"),
                ensureNotNull(step.getDescription(), "description"),
                ensureNotNull(step.getOutputArtifactPath(), "outputArtifactPath"),
                step.getWaitForCompletion());
    }

    private static UUID ensureValidUUID(String input, String name) throws SignPathStepInvalidArgumentException {
        try {
            return UUID.fromString(ensureNotNull(input, name));
        } catch (IllegalArgumentException ex) {
            throw new SignPathStepInvalidArgumentException(name + " must be a valid uuid");
        }
    }

    private static String ensureNotNull(String input, String name) throws SignPathStepInvalidArgumentException {
        if(input == null)
            throw new SignPathStepInvalidArgumentException(name + " must be set");

        return input;
    }
}
