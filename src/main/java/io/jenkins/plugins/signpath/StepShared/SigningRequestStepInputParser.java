package io.jenkins.plugins.signpath.StepShared;

import io.jenkins.plugins.signpath.*;
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

/**
 * A utility class that "parses" and validates user-input arguments given to a step
 *
 * @throws SignPathStepInvalidArgumentException if any parameter does not fulfill the requirements
 */
public final class SigningRequestStepInputParser {
    public static ApiConfiguration ParseApiConfiguration(SignPathStepBase step) throws SignPathStepInvalidArgumentException {
        return new ApiConfiguration(
                ensureValidURL(step.getApiUrl()),
                step.getServiceUnavailableTimeoutInSeconds(),
                step.getUploadAndDownloadRequestTimeoutInSeconds(),
                step.getWaitForCompletionTimeoutInSeconds(),
                step.getWaitForPowerShellTimeoutInSeconds());
    }

    public static SubmitSigningRequestStepInput ParseInput(SubmitSigningRequestStep step) throws SignPathStepInvalidArgumentException {
        boolean waitForCompletion = step.getWaitForCompletion();
        String outputArtifactPath = waitForCompletion ? ensureNotNull(step.getOutputArtifactPath(), "outputArtifactPath") : null;

        return new SubmitSigningRequestStepInput(
                ensureValidUUID(step.getOrganizationId(), "organizationId"),
                ensureNotNull(step.getTrustedBuildSystemTokenCredentialId(), "trustedBuildSystemTokenCredentialId"),
                ensureNotNull(step.getCiUserTokenCredentialId(), "ciUserTokenCredentialId"),
                ensureNotNull(step.getProjectSlug(), "projectSlug"),
                step.getArtifactConfigurationSlug(),
                ensureNotNull(step.getSigningPolicySlug(), "signingPolicySlug"),
                ensureNotNull(step.getInputArtifactPath(), "inputArtifactPath"),
                step.getDescription(),
                outputArtifactPath,
                waitForCompletion);
    }

    public static GetSignedArtifactStepInput ParseInput(GetSignedArtifactStep step) throws SignPathStepInvalidArgumentException {
        return new GetSignedArtifactStepInput(
                ensureValidUUID(step.getOrganizationId(), "organizationId"),
                ensureValidUUID(step.getSigningRequestId(), "signingRequestId"),
                ensureNotNull(step.getTrustedBuildSystemTokenCredentialId(), "trustedBuildSystemTokenCredentialId"),
                ensureNotNull(step.getCiUserTokenCredentialId(), "ciUserTokenCredentialId"),
                ensureNotNull(step.getOutputArtifactPath(), "outputArtifactPath"));
    }

    private static URL ensureValidURL(String apiUrl) throws SignPathStepInvalidArgumentException {
        try {
            return new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new SignPathStepInvalidArgumentException(apiUrl + " must be a valid url");
        }
    }

    private static UUID ensureValidUUID(String input, String name) throws SignPathStepInvalidArgumentException {
        try {
            return UUID.fromString(ensureNotNull(input, name));
        } catch (IllegalArgumentException ex) {
            throw new SignPathStepInvalidArgumentException(name + " must be a valid uuid");
        }
    }

    private static String ensureNotNull(String input, String name) throws SignPathStepInvalidArgumentException {
        if (input == null)
            throw new SignPathStepInvalidArgumentException(name + " must be set");

        return input;
    }
}
