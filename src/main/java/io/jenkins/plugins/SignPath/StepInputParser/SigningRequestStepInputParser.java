package io.jenkins.plugins.SignPath.StepInputParser;

import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.GetSignedArtifactStep;
import io.jenkins.plugins.SignPath.SubmitSigningRequestStep;

import java.util.UUID;

public class SigningRequestStepInputParser {

    public static SubmitSigningRequestStepInput Parse(SubmitSigningRequestStep step) throws SignPathStepInvalidArgumentException {
        return new SubmitSigningRequestStepInput(
                ensureValidUUID(step.getOrganizationId(), "organizationId"),
                ensureNotNull(step.getCiUserToken(), "ciUserToken"),
                ensureNotNull(step.getProjectSlug(), "projectSlug"),
                step.getArtifactConfigurationSlug(),
                ensureNotNull(step.getSigningPolicySlug(), "signingPolicySlug"),
                ensureNotNull(step.getInputArtifactPath(), "inputArtifactPath"),
                step.getDescription(),
                ensureNotNull(step.getOutputArtifactPath(), "outputArtifactPath"),
                step.getWaitForCompletion());
    }

    public static GetSignedArtifactStepInput Parse(GetSignedArtifactStep step) throws SignPathStepInvalidArgumentException {
        return new GetSignedArtifactStepInput(
                ensureValidUUID(step.getOrganizationId(), "organizationId"),
                ensureValidUUID(step.getSigningRequestId(), "signingRequestId"),
                ensureNotNull(step.getCiUserToken(), "ciUserToken"),
                ensureNotNull(step.getOutputArtifactPath(),"outputArtifactPath"));
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
