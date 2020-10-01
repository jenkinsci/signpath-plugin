package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignPathPowerShellFacade implements ISignPathFacade {

    private final ApiConfiguration apiConfiguration;
    private final IPowerShellExecutor powerShellExecutor;
    private final SignPathCredentials credentials;

    public SignPathPowerShellFacade(IPowerShellExecutor powerShellExecutor, SignPathCredentials credentials, ApiConfiguration apiConfiguration){
        this.powerShellExecutor = powerShellExecutor;
        this.credentials = credentials;
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public TemporaryFile submitSigningRequest(SigningRequestModel submitModel) throws IOException {
        TemporaryFile outputArtifact = new TemporaryFile();
        String submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, outputArtifact);
        powerShellExecutor.execute(submitSigningRequestCommand);
        return outputArtifact;
    }

    @Override
    public UUID submitSigningRequestAsync(SigningRequestModel submitModel) {
        String submitSigningRequestCommand = createSubmitSigningRequestCommand(submitModel, null);
        PowerShellExecutionResult result = powerShellExecutor.execute(submitSigningRequestCommand);
        return extractSigningRequestId(result.getOutput());
    }

    private UUID extractSigningRequestId(String output){
        // Last output line = return value => we want the PowerShell script to return a GUID
        final String guidRegex = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$";
        Matcher regexResult = Pattern.compile(guidRegex, Pattern.MULTILINE).matcher(output);
        assert regexResult.find(); // TODO SIGN-3326: throw correct exception
        String signingRequestId = regexResult.group(0);
        return UUID.fromString(signingRequestId);
    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException {
        TemporaryFile outputArtifact = new TemporaryFile();
        String getSignedArtifactCommand = createGetSignedArtifactCommand(organizationId, signingRequestID,  outputArtifact);
        powerShellExecutor.execute(getSignedArtifactCommand);
        return outputArtifact;
    }

    private String createSubmitSigningRequestCommand(SigningRequestModel signingRequestModel, TemporaryFile outputArtifact){
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
        argumentsBuilder.append(String.format("'RepositoryMetadata.RepositoryUrl)' = '%s';", repositoryMetadata.getRepositoryUrl()));
        argumentsBuilder.append(String.format("'RepositoryMetadata.SourceControlManagementType)' = '%s'", repositoryMetadata.getSourceControlManagementType()));
        argumentsBuilder.append("}");

        if (outputArtifact != null)
        {
            argumentsBuilder.append("-WaitForCompletion ");
            argumentsBuilder.append(String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()));
            argumentsBuilder.append(String.format( "-WaitForCompletionTimeoutInSeconds '%s'",apiConfiguration.getWaitForCompletionTimeoutInSeconds()));
            argumentsBuilder.append("-Force ");
        }

        return argumentsBuilder.toString();
    }

    private String createGetSignedArtifactCommand(UUID organizationId, UUID signingRequestId, TemporaryFile outputArtifact){
        return "Submit-SigningRequest " + String.format("-ApiUrl '%s' ", apiConfiguration.getApiUrl()) +
                String.format("-CIUserToken '%s' ", credentials.toString()) +
                String.format("-OrganizationId '%s' ", organizationId) +
                String.format("-SigningRequestId '%s' ", signingRequestId) +
                String.format("-ServiceUnavailableTimeoutInSeconds '%s' ", apiConfiguration.getServiceUnavailableTimeoutInSeconds()) +
                String.format("-UploadAndDownloadRequestTimeoutInSeconds '%s' ", apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()) +
                String.format("-OutputArtifactPath '%s' ", outputArtifact.getAbsolutePath()) +
                String.format("-WaitForCompletionTimeoutInSeconds '%s'", apiConfiguration.getWaitForCompletionTimeoutInSeconds()) +
                "-Force ";
    }
}
