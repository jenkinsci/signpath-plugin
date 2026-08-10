package io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacade;
//</editor-fold>
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.Model.ConnectorSigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.Common.PluginConstants;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.PipelineConnectorFacadeCallException;
import io.signpath.signpathclient.connector.PipelineConnectorClient;
import io.signpath.signpathclient.connector.model.ConnectorLogEntry;
import io.signpath.signpathclient.connector.model.ConnectorSigningRequestStatusResponse;
import io.signpath.signpathclient.connector.model.ConnectorSigningRequestSubmitRequest;
import io.signpath.signpathclient.connector.model.ConnectorSubmitSigningRequestResponse;
import io.signpath.signpathclient.connector.model.ConnectorUserDefinedParameter;
import io.signpath.signpathclient.SignPathClientSettings;
import io.signpath.signpathclient.SignPathClientException;
import io.signpath.signpathclient.SignPathClientSimpleLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public class PipelineConnectorFacade implements IPipelineConnectorFacade {

    private final PipelineConnectorClient client;
    private final SignPathCredentials credentials;
    private final SignPathClientSimpleLogger logger;
    private final String endpointSlug;

    public PipelineConnectorFacade(SignPathCredentials credentials, ApiConfiguration apiConfiguration, SignPathClientSimpleLogger logger) {
        this.credentials = credentials;
        this.logger = logger;
        this.endpointSlug = apiConfiguration.getEndpointSlug();
        String baseUrl = apiConfiguration.getConnectorUrl().toString();
        if(!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        this.client = new PipelineConnectorClient(baseUrl, logger,
            new SignPathClientSettings(
                apiConfiguration.getServiceUnavailableTimeoutInSeconds(),
                apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds(),
                apiConfiguration.getWaitForCompletionTimeoutInSeconds(),
                apiConfiguration.getWaitBetweenReadinessChecksInSeconds(),
                buildUserAgent()
            ), apiConfiguration.getServiceUnavailableTimeoutInSeconds());
    }

    @Override
    public SubmitSigningRequestResult submitSigningRequest(ConnectorSigningRequestModel submitModel) throws PipelineConnectorFacadeCallException {
        try {
            ConnectorSigningRequestSubmitRequest request = buildSubmitRequest(submitModel);
            ConnectorSubmitSigningRequestResponse response = this.client.submitSigningRequest(
                    credentials.getApiToken().getPlainText(),
                    PluginConstants.BUILD_SYSTEM_TYPE,
                    endpointSlug,
                    submitModel.getOrganizationId().toString(),
                    request);

            logConnectorLogs(response.getLogs());

            if (response.getSigningRequestId() == null || response.getSigningRequestId().isEmpty()) {
                String message = (response.getError() != null && !response.getError().isEmpty())
                        ? response.getError()
                        : "The connector did not return a signing request id";
                throw new PipelineConnectorFacadeCallException(message);
            }

            String artifactUploadLink = response.getArtifactUploadLink();
            // When an input artifact retrieval URL is used, SignPath downloads the artifact itself
            // and the connector does not return an upload link.
            boolean uploadLinkExpected = emptyToNull(submitModel.getInputArtifactRetrievalUrl()) == null;
            if (uploadLinkExpected && (artifactUploadLink == null || artifactUploadLink.isEmpty())) {
                String message = (response.getError() != null && !response.getError().isEmpty())
                        ? response.getError()
                        : "The connector did not return an artifact upload link";
                throw new PipelineConnectorFacadeCallException(message);
            }

            return new SubmitSigningRequestResult(
                    UUID.fromString(response.getSigningRequestId()),
                    response.getSigningRequestUrl(),
                    artifactUploadLink);
        } catch (SignPathClientException ex) {
            Logger.getLogger(PipelineConnectorFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new PipelineConnectorFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public void uploadUnsignedArtifact(UUID organizationId, UUID signingRequestId, String artifactUploadLink, File unsignedArtifact) throws PipelineConnectorFacadeCallException {
        if (artifactUploadLink == null || artifactUploadLink.isEmpty()) {
            throw new PipelineConnectorFacadeCallException("The connector did not return an artifact upload link");
        }

        try {
            client.uploadUnsignedArtifact(
                    credentials.getApiToken().getPlainText(),
                    PluginConstants.BUILD_SYSTEM_TYPE,
                    endpointSlug,
                    organizationId.toString(),
                    signingRequestId.toString(),
                    artifactUploadLink,
                    unsignedArtifact);
        } catch (SignPathClientException ex) {
            Logger.getLogger(PipelineConnectorFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new PipelineConnectorFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public void waitForFinalSigningRequestStatus(UUID organizationId, UUID signingRequestId) throws PipelineConnectorFacadeCallException {
        try {
            ConnectorSigningRequestStatusResponse statusResponse = client.waitForFinalSigningRequestStatus(
                    credentials.getApiToken().getPlainText(),
                    PluginConstants.BUILD_SYSTEM_TYPE,
                    endpointSlug,
                    organizationId.toString(),
                    signingRequestId.toString());
            if (!statusResponse.isFinalStatus()) {
                throw new PipelineConnectorFacadeCallException("Timeout expired while waiting for signing request to complete");
            }
        } catch (SignPathClientException ex) {
            Logger.getLogger(PipelineConnectorFacade.class.getName()).log(Level.SEVERE, null, ex);
            throw new PipelineConnectorFacadeCallException(ex.getMessage());
        }
    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, PipelineConnectorFacadeCallException {
        TemporaryFile outputArtifact = new TemporaryFile();

        try {
            waitForFinalSigningRequestStatus(organizationId, signingRequestID);

            client.downloadSignedArtifact(
                    credentials.getApiToken().getPlainText(),
                    PluginConstants.BUILD_SYSTEM_TYPE,
                    endpointSlug,
                    organizationId.toString(),
                    signingRequestID.toString(),
                    outputArtifact.getFile());
            return outputArtifact;
        }
        catch (SignPathClientException ex) {
            throw new PipelineConnectorFacadeCallException(ex.getMessage());
        }
    }

    private ConnectorSigningRequestSubmitRequest buildSubmitRequest(ConnectorSigningRequestModel submitModel) {
        ConnectorSigningRequestSubmitRequest request = new ConnectorSigningRequestSubmitRequest();
        request.jobFullName = submitModel.getJobFullName();
        request.buildNumber = submitModel.getBuildNumber();
        request.sha256ArtifactPath = submitModel.getSha256ArtifactPath();
        request.signPathProjectSlug = submitModel.getProjectSlug();
        request.signPathSigningPolicySlug = submitModel.getSigningPolicySlug();
        request.signPathArtifactConfigurationSlug = emptyToNull(submitModel.getArtifactConfigurationSlug());
        request.signingRequestDescription = emptyToNull(submitModel.getDescription());
        request.inputArtifactRetrievalUrl = emptyToNull(submitModel.getInputArtifactRetrievalUrl());
        Map<String, String> httpHeaders = submitModel.getInputArtifactRetrievalHttpHeaders();
        request.inputArtifactRetrievalHttpHeaders = (httpHeaders != null && !httpHeaders.isEmpty()) ? httpHeaders : null;
        request.parameters = toParameterList(submitModel.getParameters());
        return request;
    }

    private static List<ConnectorUserDefinedParameter> toParameterList(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        List<ConnectorUserDefinedParameter> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            result.add(new ConnectorUserDefinedParameter(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    private void logConnectorLogs(List<ConnectorLogEntry> logs) {
        if (logs == null) {
            return;
        }
        for (ConnectorLogEntry entry : logs) {
            if (entry != null && entry.getMessage() != null && !entry.getMessage().isEmpty()) {
                logger.log(String.format("[CONNECTOR] %s", entry.getMessage()));
            }
        }
    }

    private String buildUserAgent(){
        return String.format("SignPath.Plugins.Jenkins/%1$s (OpenJDK %2$s; Jenkins %3$s)",
                PipelineConnectorFacade.class.getPackage().getImplementationVersion(),
                System.getProperty("java.version"),
                Jenkins.getVersion());
    }
}
