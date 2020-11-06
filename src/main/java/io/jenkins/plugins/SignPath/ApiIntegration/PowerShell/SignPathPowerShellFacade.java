package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
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
        String submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, outputArtifact);
        executePowerShellSafe(submitSigningRequestCommand);
        return outputArtifact;
    }

    @Override
    public UUID submitSigningRequestAsync(SigningRequestModel submitModel) throws SignPathFacadeCallException {
        String submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, null);
        String result = executePowerShellSafe(submitSigningRequestCommand);
        return extractSigningRequestId(result);
    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();
        String getSignedArtifactCommand = createGetSignedArtifactCommand(organizationId, signingRequestID, outputArtifact);
        executePowerShellSafe(getSignedArtifactCommand);
        return outputArtifact;
    }

    private String executePowerShellSafe(String command) throws SignPathFacadeCallException {
        PowerShellExecutionResult result = powerShellExecutor.execute(command, apiConfiguration.getWaitForPowerShellTimeoutInSeconds());

        String output = result.getOutput();

        if (result.getHasError())
            throw new SignPathFacadeCallException(String.format("PowerShell script exited with error: '%s'", output));
        else
            logger.printf("PowerShell script ran successfully with the following output:\n%s\n", output);

        return output;
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

    private String createSubmitSigningRequestCommand(SigningRequestModel signingRequestModel, TemporaryFile outputArtifact) {
        StringBuilder argumentsBuilder = new StringBuilder("Submit-SigningRequest ");
        argumentsBuilder.append(String.format("-ApiUrl '%s' ", apiConfiguration.getApiUrl()));
        argumentsBuilder.append(String.format("-CIUserToken '%s' ", credentials.toString()));
        argumentsBuilder.append(String.format("-OrganizationId '%s' ", signingRequestModel.getOrganizationId()));
        argumentsBuilder.append(String.format("-InputArtifactPath '%s' ", signingRequestModel.getArtifact().getAbsolutePath()));
        argumentsBuilder.append(String.format("-ProjectSlug '%s' ", signingRequestModel.getProjectSlug()));
        argumentsBuilder.append(String.format("-SigningPolicySlug '%s' ", signingRequestModel.getSigningPolicySlug()));

        if (signingRequestModel.getArtifactConfigurationSlug() != null)
            argumentsBuilder.append(String.format("-ArtifactConfigurationSlug '%s' ", signingRequestModel.getArtifactConfigurationSlug()));

        if (signingRequestModel.getDescription() != null)
            argumentsBuilder.append(String.format("-Description '%s' ", signingRequestModel.getDescription()));

        argumentsBuilder.append(String.format("-ServiceUnavailableTimeoutInSeconds '%s' ", apiConfiguration.getServiceUnavailableTimeoutInSeconds()));
        argumentsBuilder.append(String.format("-UploadAndDownloadRequestTimeoutInSeconds '%s' ", apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()));
        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();
        argumentsBuilder.append("-Origin @{");
        argumentsBuilder.append(String.format("'BuildUrl' = '%s';", origin.getBuildUrl()));
        argumentsBuilder.append(String.format("'BuildSettingsFile' = '%s';", origin.getBuildSettingsFile().getAbsolutePath()));
        argumentsBuilder.append(String.format("'RepositoryMetadata.BranchName' = '%s';", repositoryMetadata.getBranchName()));
        argumentsBuilder.append(String.format("'RepositoryMetadata.CommitId' = '%s';", repositoryMetadata.getCommitId()));
        argumentsBuilder.append(String.format("'RepositoryMetadata.RepositoryUrl' = '%s';", repositoryMetadata.getRepositoryUrl()));
        argumentsBuilder.append(String.format("'RepositoryMetadata.SourceControlManagementType' = '%s'", repositoryMetadata.getSourceControlManagementType()));
        argumentsBuilder.append("} ");

        if (outputArtifact != null) {
            argumentsBuilder.append("-WaitForCompletion ");
            argumentsBuilder.append(String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()));
            argumentsBuilder.append(String.format("-WaitForCompletionTimeoutInSeconds '%s' ", apiConfiguration.getWaitForCompletionTimeoutInSeconds()));
            argumentsBuilder.append("-Force ");
        }

        return argumentsBuilder.toString();
    }

    private String createGetSignedArtifactCommand(UUID organizationId, UUID signingRequestId, TemporaryFile outputArtifact) {
        return "Get-SignedArtifact " + String.format("-ApiUrl '%s' ", apiConfiguration.getApiUrl()) +
                String.format("-CIUserToken '%s' ", credentials.toString()) +
                String.format("-OrganizationId '%s' ", organizationId) +
                String.format("-SigningRequestId '%s' ", signingRequestId) +
                String.format("-ServiceUnavailableTimeoutInSeconds '%s' ", apiConfiguration.getServiceUnavailableTimeoutInSeconds()) +
                String.format("-UploadAndDownloadRequestTimeoutInSeconds '%s' ", apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()) +
                String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()) +
                String.format("-WaitForCompletionTimeoutInSeconds '%s' ", apiConfiguration.getWaitForCompletionTimeoutInSeconds()) +
                "-Force ";
    }
}