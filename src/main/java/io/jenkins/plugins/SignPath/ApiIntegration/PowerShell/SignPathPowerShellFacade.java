package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;

import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public TemporaryFile submitSigningRequest(SigningRequestModel submitModel) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();
        PowerShellCommand submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, outputArtifact);
        executePowerShellSafe(submitSigningRequestCommand);
        return outputArtifact;
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
        // Last output line = return value => we want the PowerShell script to return a GUID
        final String guidRegex = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$";
        Matcher regexResult = Pattern.compile(guidRegex, Pattern.MULTILINE).matcher(output);
        if (!regexResult.find()) {
            throw new SignPathFacadeCallException("Unexpected output from PowerShell, did not find a valid signingRequestId.");
        }
        String signingRequestId = regexResult.group(0);
        return UUID.fromString(signingRequestId);
    }

    private PowerShellCommand createSubmitSigningRequestCommand(SigningRequestModel signingRequestModel, TemporaryFile outputArtifact) {
        PowerShellCommandBuilder commandBuilder = new PowerShellCommandBuilder("Submit-SigningRequest");
//        commandBuilder.append(String.format("-ApiUrl '%s' ", apiConfiguration.getApiUrl()));
//        commandBuilder.append(String.format("-CIUserToken '%s' ", credentials.toString()));
//        commandBuilder.append(String.format("-OrganizationId '%s' ", signingRequestModel.getOrganizationId()));
//        commandBuilder.append(String.format("-InputArtifactPath '%s' ", signingRequestModel.getArtifact().getAbsolutePath()));
//        commandBuilder.append(String.format("-ProjectSlug '%s' ", signingRequestModel.getProjectSlug()));
//        commandBuilder.append(String.format("-SigningPolicySlug '%s' ", signingRequestModel.getSigningPolicySlug()));
//
//        if (signingRequestModel.getArtifactConfigurationSlug() != null)
//            commandBuilder.append(String.format("-ArtifactConfigurationSlug '%s' ", signingRequestModel.getArtifactConfigurationSlug()));
//
//        if (signingRequestModel.getDescription() != null)
//            commandBuilder.append(String.format("-Description '%s' ", signingRequestModel.getDescription()));
//
//        commandBuilder.append(String.format("-ServiceUnavailableTimeoutInSeconds '%s' ", apiConfiguration.getServiceUnavailableTimeoutInSeconds()));
//        commandBuilder.append(String.format("-UploadAndDownloadRequestTimeoutInSeconds '%s' ", apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()));
//        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
//        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();
//        commandBuilder.append("-Origin @{");
//        commandBuilder.append(String.format("'BuildUrl' = '%s';", origin.getBuildUrl()));
//        commandBuilder.append(String.format("'BuildSettingsFile' = '%s';", origin.getBuildSettingsFile().getAbsolutePath()));
//        commandBuilder.append(String.format("'RepositoryMetadata.BranchName' = '%s';", repositoryMetadata.getBranchName()));
//        commandBuilder.append(String.format("'RepositoryMetadata.CommitId' = '%s';", repositoryMetadata.getCommitId()));
//        commandBuilder.append(String.format("'RepositoryMetadata.RepositoryUrl' = '%s';", repositoryMetadata.getRepositoryUrl()));
//        commandBuilder.append(String.format("'RepositoryMetadata.SourceControlManagementType' = '%s'", repositoryMetadata.getSourceControlManagementType()));
//        commandBuilder.append("} ");
//
//        if (outputArtifact != null) {
//            commandBuilder.append("-WaitForCompletion ");
//            commandBuilder.append(String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()));
//            commandBuilder.append(String.format("-WaitForCompletionTimeoutInSeconds '%s' ", apiConfiguration.getWaitForCompletionTimeoutInSeconds()));
//            commandBuilder.append("-Force ");
//        }

        return commandBuilder.build();
    }

    private PowerShellCommand createGetSignedArtifactCommand(UUID organizationId, UUID signingRequestId, TemporaryFile outputArtifact) {
        PowerShellCommandBuilder commandBuilder = new PowerShellCommandBuilder("Get-SignedArtifact");
        return commandBuilder.build();
//        return "Get-SignedArtifact " + String.format("-ApiUrl '%s' ", apiConfiguration.getApiUrl()) +
//                String.format("-CIUserToken '%s' ", credentials.toString()) +
//                String.format("-OrganizationId '%s' ", organizationId) +
//                String.format("-SigningRequestId '%s' ", signingRequestId) +
//                String.format("-ServiceUnavailableTimeoutInSeconds '%s' ", apiConfiguration.getServiceUnavailableTimeoutInSeconds()) +
//                String.format("-UploadAndDownloadRequestTimeoutInSeconds '%s' ", apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()) +
//                String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()) +
//                String.format("-WaitForCompletionTimeoutInSeconds '%s' ", apiConfiguration.getWaitForCompletionTimeoutInSeconds()) +
//                "-Force ";
    }
}