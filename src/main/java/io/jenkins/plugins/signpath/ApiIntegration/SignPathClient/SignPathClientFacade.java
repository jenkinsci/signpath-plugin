package io.jenkins.plugins.signpath.ApiIntegration.SignPathClient;
//</editor-fold>
import hudson.util.VersionNumber;
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.signpath.signpathclient.ClientSettings;
import io.signpath.signpathclient.SignPathClient;
import io.signpath.signpathclient.SignPathClientException;
import io.signpath.signpathclient.api.model.SigningRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public class SignPathClientFacade implements SignPathFacade {

    private final SignPathClient client;
    private final SignPathCredentials credentials;
    private final ApiConfiguration apiConfiguration;
    private final PrintStream logger;

    public SignPathClientFacade(SignPathCredentials credentials, ApiConfiguration apiConfiguration, PrintStream logger) {
        this.credentials = credentials;
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
        String baseUrl = apiConfiguration.getApiUrl().toString();
        if(!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        this.client = new SignPathClient(baseUrl, logger,
            new ClientSettings(
                apiConfiguration.getServiceUnavailableTimeoutInSeconds(),
                apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds(),
                apiConfiguration.getWaitForCompletionTimeoutInSeconds(),
                apiConfiguration.getWaitBetweenReadinessChecksInSeconds(),
                buildUserAgent()
            ));
    }

    @Override
    public SubmitSigningRequestResult submitSigningRequest(SigningRequestModel submitModel) throws IOException, SignPathFacadeCallException {
        try {
            TemporaryFile outputArtifact = new TemporaryFile();
            
            String requestId = this.client.submitSigningRequestAndWaitForSignedArtifact(
                    credentials.getApiToken().getPlainText(),
                    credentials.getTrustedBuildSystemToken().getPlainText(),
                    submitModel.getOrganizationId().toString(),
                    submitModel.getArtifact().getFile(),
                    submitModel.getProjectSlug(),
                    submitModel.getSigningPolicySlug(),
                    submitModel.getArtifactConfigurationSlug(),
                    outputArtifact.getFile(),
                    submitModel.getDescription(), 
                    buildOriginData(submitModel)
                    );

            return new SubmitSigningRequestResult(outputArtifact, UUID.fromString(requestId));
        } catch (SignPathClientException ex) {
            Logger.getLogger(SignPathClientFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new SignPathFacadeCallException(ex.getMessage());
        } catch (InterruptedException ex) {
            Logger.getLogger(SignPathClientFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new SignPathFacadeCallException("An unhandled exception occurred while submitting a signing request");
        }
    }

    @Override
    public UUID submitSigningRequestAsync(SigningRequestModel submitModel) throws SignPathFacadeCallException {
        
        String requestId = this.client.submitSigningRequest(
                credentials.getApiToken().getPlainText(),
                credentials.getTrustedBuildSystemToken().getPlainText(),
                submitModel.getOrganizationId().toString(),
                submitModel.getArtifact().getFile(),
                submitModel.getProjectSlug(),
                submitModel.getSigningPolicySlug(),
                submitModel.getArtifactConfigurationSlug(),
                submitModel.getDescription(),
                buildOriginData(submitModel));
        return UUID.fromString(requestId);
    }
    
    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();
        
        try {
            SigningRequest request = client.getSigningRequestWaitForFinalStatus(
                credentials.getApiToken().getPlainText(),
                credentials.getTrustedBuildSystemToken().getPlainText(),
                organizationId.toString(),
                signingRequestID.toString());
        
            if(!request.isFinalStatus()) {
                throw new SignPathFacadeCallException("Timeout expired while waiting for signing request to complete");
            }

            client.downloadSignedArtifact(
                    credentials.getApiToken().getPlainText(),
                    credentials.getTrustedBuildSystemToken().getPlainText(),
                    organizationId.toString(),
                    signingRequestID.toString(),
                    outputArtifact.getFile());
            return outputArtifact;
        }
        catch (SignPathClientException ex) {
            throw new SignPathFacadeCallException(ex.getMessage());
        }
    }
    
    private Map<String, String> buildOriginData(SigningRequestModel submitModel){
        Map<String, String> originParameters = new HashMap<>();
        SigningRequestOriginModel origin = submitModel.getOrigin();

        originParameters.put("BuildData.Url", origin.getBuildUrl());
        originParameters.put("BuildData.BuildSettingsFile", String.format("@%s", origin.getBuildSettingsFile().getAbsolutePath()));
        originParameters.put("RepositoryData.BranchName", origin.getRepositoryMetadata().getBranchName());
        originParameters.put("RepositoryData.CommitId", origin.getRepositoryMetadata().getCommitId());
        originParameters.put("RepositoryData.Url", origin.getRepositoryMetadata().getRepositoryUrl());
        originParameters.put("RepositoryData.SourceControlManagementType", origin.getRepositoryMetadata().getSourceControlManagementType());
        
        return originParameters;
    }
    
    private String buildUserAgent(){
        
        return String.format("SignPath.Plugins.Jenkins/%1$s (OpenJDK %2$s; Jenkins %3$s)",
                SignPathClientFacade.class.getPackage().getImplementationVersion(),
                System.getProperty("java.version"),
                Jenkins.getVersion()
                );
    }
}