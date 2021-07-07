package io.jenkins.plugins.signpath.ApiIntegration.PowerShell;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An implementation of the
 *
 * @see SignPathFacade interface
 * that delegates to the local SignPath PowerShell Module
 * A version of the Module must be installed in the local powershell / powershell core
 */
public class SignPathPowerShellFacade implements SignPathFacade {

    private final PowerShellExecutor powerShellExecutor;
    private final SignPathCredentials credentials;
    private final ApiConfiguration apiConfiguration;
    private final PrintStream logger;

    public SignPathPowerShellFacade(PowerShellExecutor powerShellExecutor, SignPathCredentials credentials, ApiConfiguration apiConfiguration, PrintStream logger) {
        this.powerShellExecutor = powerShellExecutor;
        this.credentials = credentials;
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public SubmitSigningRequestResult submitSigningRequest(SigningRequestModel submitModel) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();
        PowerShellCommand submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, outputArtifact);
        String result = executePowerShellSafe(submitSigningRequestCommand);
        return new SubmitSigningRequestResult(outputArtifact, extractSigningRequestId(result));
    }

    @Override
    public UUID submitSigningRequestAsync(SigningRequestModel submitModel) throws SignPathFacadeCallException {
        PowerShellCommand submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, null);
        String result = executePowerShellSafe(submitSigningRequestCommand);
        return extractSigningRequestId(result);
    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();
        PowerShellCommand getSignedArtifactCommand = createGetSignedArtifactCommand(organizationId, signingRequestID, outputArtifact);
        executePowerShellSafe(getSignedArtifactCommand);
        return outputArtifact;
    }

    private String executePowerShellSafe(PowerShellCommand command) throws SignPathFacadeCallException {
        PowerShellExecutionResult result = powerShellExecutor.execute(command, apiConfiguration.getWaitForPowerShellTimeoutInSeconds());

        if (result.getHasError())
            throw new SignPathFacadeCallException(String.format("PowerShell script exited with error: '%s'", result.getErrorDescription()));

        logger.println("PowerShell script ran successfully.");
        return result.getOutput();
    }

    private UUID extractSigningRequestId(String output) throws SignPathFacadeCallException {
        // we assume the last line is the result = uuid
        String[] lines = output.split(System.getProperty("line.separator"));
        String lastLine = lines[lines.length - 1];
        try {
            return UUID.fromString(lastLine);
        } catch (IllegalArgumentException ex) {
            throw new SignPathFacadeCallException("Unexpected output from PowerShell, did not find a valid signingRequestId.");
        }
    }

    private PowerShellCommand createSubmitSigningRequestCommand(SigningRequestModel signingRequestModel, TemporaryFile outputArtifact) {
        PowerShellCommandBuilder commandBuilder = new PowerShellCommandBuilder("Submit-SigningRequest");
        commandBuilder.appendParameter("ApiUrl", apiConfiguration.getApiUrl().toString());
        commandBuilder.appendParameter("CIUserToken", credentials.toCredentialString());
        commandBuilder.appendParameter("OrganizationId", signingRequestModel.getOrganizationId().toString());
        commandBuilder.appendParameter("InputArtifactPath", signingRequestModel.getArtifact().getAbsolutePath());
        commandBuilder.appendParameter("ProjectSlug", signingRequestModel.getProjectSlug());
        commandBuilder.appendParameter("SigningPolicySlug", signingRequestModel.getSigningPolicySlug());

        if (signingRequestModel.getArtifactConfigurationSlug() != null)
            commandBuilder.appendParameter("ArtifactConfigurationSlug", signingRequestModel.getArtifactConfigurationSlug());

        if (signingRequestModel.getDescription() != null)
            commandBuilder.appendParameter("Description", signingRequestModel.getDescription());

        commandBuilder.appendParameter("ServiceUnavailableTimeoutInSeconds", String.valueOf(apiConfiguration.getServiceUnavailableTimeoutInSeconds()));
        commandBuilder.appendParameter("UploadAndDownloadRequestTimeoutInSeconds", String.valueOf(apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()));

        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();

        Map<String, String> originParameters = new HashMap<>();
        originParameters.put("BuildUrl", origin.getBuildUrl());
        originParameters.put("BuildSettingsFile", String.format("@%s", origin.getBuildSettingsFile().getAbsolutePath()));
        originParameters.put("BranchName", repositoryMetadata.getBranchName());
        originParameters.put("CommitId", repositoryMetadata.getCommitId());
        originParameters.put("RepositoryUrl", repositoryMetadata.getRepositoryUrl());
        originParameters.put("SourceControlManagementType", repositoryMetadata.getSourceControlManagementType());
        commandBuilder.appendCustom("-Origin @{'BuildData' = @{" +
                "'Url' = \"$($env:BuildUrl)\";" +
                "'BuildSettingsFile' = \"$($env:BuildSettingsFile)\";" +
                "};" +
                "'RepositoryData' = @{" +
                "'BranchName' = \"$($env:BranchName)\";" +
                "'CommitId' = \"$($env:CommitId)\";" +
                "'Url' = \"$($env:RepositoryUrl)\";" +
                "'SourceControlManagementType' = \"$($env:SourceControlManagementType)\"" +
                "}}", originParameters);

        if (outputArtifact != null) {
            commandBuilder.appendFlag("WaitForCompletion");
            commandBuilder.appendParameter("OutputArtifactPath", outputArtifact.getAbsolutePath());
            commandBuilder.appendParameter("WaitForCompletionTimeoutInSeconds", String.valueOf(apiConfiguration.getWaitForCompletionTimeoutInSeconds()));
            commandBuilder.appendFlag("Force");
        }

        commandBuilder.appendFlag("Verbose");
        return commandBuilder.build();
    }

    private PowerShellCommand createGetSignedArtifactCommand(UUID organizationId, UUID signingRequestId, TemporaryFile outputArtifact) {
        PowerShellCommandBuilder commandBuilder = new PowerShellCommandBuilder("Get-SignedArtifact");
        commandBuilder.appendParameter("ApiUrl", apiConfiguration.getApiUrl().toString());
        commandBuilder.appendParameter("CIUserToken", credentials.toCredentialString().getPlainText());
        commandBuilder.appendParameter("OrganizationId", organizationId.toString());
        commandBuilder.appendParameter("SigningRequestId", signingRequestId.toString());
        commandBuilder.appendParameter("ServiceUnavailableTimeoutInSeconds", String.valueOf(apiConfiguration.getServiceUnavailableTimeoutInSeconds()));
        commandBuilder.appendParameter("UploadAndDownloadRequestTimeoutInSeconds", String.valueOf(apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()));
        commandBuilder.appendParameter("OutputArtifactPath", outputArtifact.getAbsolutePath());
        commandBuilder.appendParameter("WaitForCompletionTimeoutInSeconds", String.valueOf(apiConfiguration.getWaitForCompletionTimeoutInSeconds()));
        commandBuilder.appendFlag("Force");
        commandBuilder.appendFlag("Verbose");
        return commandBuilder.build();
    }
}