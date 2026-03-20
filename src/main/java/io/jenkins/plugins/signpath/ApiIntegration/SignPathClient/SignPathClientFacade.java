package io.jenkins.plugins.signpath.ApiIntegration.SignPathClient;
//</editor-fold>
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithArtifactRetrievalLinkModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithoutArtifactModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithArtifactRetrievalLinkResult;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithoutArtifactResult;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.signpath.signpathclient.api.model.SigningRequestSubmitWithArtifactRetrievalLinkResponse;
import io.signpath.signpathclient.api.model.SigningRequestSubmitWithoutArtifactResponse;
import io.signpath.signpathclient.SignPathClientSettings;
import io.signpath.signpathclient.SignPathClient;
import io.signpath.signpathclient.SignPathClientException;
import io.signpath.signpathclient.SignPathClientSimpleLogger;
import io.signpath.signpathclient.api.model.SigningRequestStatusResponse;

import java.io.IOException;
import java.io.InputStream;
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
    private final SignPathClientSimpleLogger logger;

    public SignPathClientFacade(SignPathCredentials credentials, ApiConfiguration apiConfiguration, SignPathClientSimpleLogger logger) {
        this.credentials = credentials;
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
        String baseUrl = apiConfiguration.getApiUrl().toString();
        if(!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        this.client = new SignPathClient(baseUrl, logger,
            new SignPathClientSettings(
                apiConfiguration.getServiceUnavailableTimeoutInSeconds(),
                apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds(),
                apiConfiguration.getWaitForCompletionTimeoutInSeconds(),
                apiConfiguration.getWaitBetweenReadinessChecksInSeconds(),
                buildUserAgent()
            ), apiConfiguration.getServiceUnavailableTimeoutInSeconds());
    }

    @Override
    public SubmitSigningRequestWithoutArtifactResult submitSigningRequestWithoutArtifact(SigningRequestWithoutArtifactModel submitModel) throws SignPathFacadeCallException {
        try {
            SigningRequestSubmitWithoutArtifactResponse response = this.client.submitWithoutArtifact(
                    credentials.getApiToken().getPlainText(),
                    credentials.getTrustedBuildSystemToken().getPlainText(),
                    submitModel.getOrganizationId().toString(),
                    submitModel.getFileName(),
                    submitModel.getSha256HexHash(),
                    submitModel.getProjectSlug(),
                    submitModel.getSigningPolicySlug(),
                    submitModel.getArtifactConfigurationSlug(),
                    submitModel.getDescription(),
                    true,
                    buildOriginData(submitModel.getOrigin()),
                    submitModel.getParameters());

            return new SubmitSigningRequestWithoutArtifactResult(
                    UUID.fromString(response.getSigningRequestId()),
                    response.getUploadLink(),
                    response.getWebLink());
        } catch (SignPathClientException ex) {
            Logger.getLogger(SignPathClientFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new SignPathFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public SubmitSigningRequestWithArtifactRetrievalLinkResult submitSigningRequestWithArtifactRetrievalLink(SigningRequestWithArtifactRetrievalLinkModel submitModel) throws SignPathFacadeCallException {
        try {
            SigningRequestSubmitWithArtifactRetrievalLinkResponse response = this.client.submitWithArtifactRetrievalLink(
                    credentials.getApiToken().getPlainText(),
                    credentials.getTrustedBuildSystemToken().getPlainText(),
                    submitModel.getOrganizationId().toString(),
                    submitModel.getFileName(),
                    submitModel.getSha256HexHash(),
                    submitModel.getProjectSlug(),
                    submitModel.getSigningPolicySlug(),
                    submitModel.getArtifactConfigurationSlug(),
                    submitModel.getDescription(),
                    true,
                    buildOriginData(submitModel.getOrigin()),
                    submitModel.getParameters(),
                    submitModel.getRetrievalUrl(),
                    submitModel.getRetrievalHttpHeaders());

            return new SubmitSigningRequestWithArtifactRetrievalLinkResult(
                    UUID.fromString(response.getSigningRequestId()),
                    response.getWebLink());
        } catch (SignPathClientException ex) {
            Logger.getLogger(SignPathClientFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new SignPathFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public void uploadUnsignedArtifact(String uploadLink, InputStream artifactStream) throws IOException, SignPathFacadeCallException {
        try (TemporaryFile tempFile = new TemporaryFile()) {
            tempFile.copyFrom(artifactStream);
            client.uploadUnsignedArtifact(credentials.getApiToken().getPlainText(), uploadLink, tempFile.getFile());
        } catch (SignPathClientException ex) {
            Logger.getLogger(SignPathClientFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new SignPathFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();

        try {
            SigningRequestStatusResponse request = client.waitForFinalSigningRequestStatus(
                credentials.getApiToken().getPlainText(),
                organizationId.toString(),
                signingRequestID.toString());

            if(!request.isFinalStatus()) {
                throw new SignPathFacadeCallException("Timeout expired while waiting for signing request to complete");
            }

            client.downloadSignedArtifact(
                    credentials.getApiToken().getPlainText(),
                    organizationId.toString(),
                    signingRequestID.toString(),
                    outputArtifact.getFile());
            return outputArtifact;
        }
        catch (SignPathClientException ex) {
            throw new SignPathFacadeCallException(ex.getMessage());
        }
    }

    private Map<String, String> buildOriginData(SigningRequestOriginModel origin) {
        Map<String, String> originParameters = new HashMap<>();

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
                Jenkins.getVersion());
    }
}
