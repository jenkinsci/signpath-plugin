package io.jenkins.plugins.signpath.ApiIntegration;

import io.jenkins.plugins.signpath.ApiIntegration.Model.ConnectorSigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.PipelineConnectorFacadeCallException;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * A facade for the SignPath Pipeline Connector
 */
public interface IPipelineConnectorFacade {

    /**
     * Submits a signing request to the SignPath Pipeline Connector. The connector pulls the
     * SHA-256 sidecar from the Jenkins build's archived artifacts and forwards the request to the SignPath Application.
     *
     * @param submitModel all the (non-authentication) parameters including job/build identity and slugs
     * @return the signing request ID, web link and (unless an input artifact retrieval URL is used) the artifact upload link
     * @throws PipelineConnectorFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    SubmitSigningRequestResult submitSigningRequest(ConnectorSigningRequestModel submitModel) throws PipelineConnectorFacadeCallException;

    /**
     * Uploads the unsigned artifact for a previously submitted signing request.
     *
     * @param organizationId     the organization ID where the signing request resides
     * @param signingRequestId   the signing request ID as returned by submitSigningRequest
     * @param artifactUploadLink the upload link as returned by submitSigningRequest
     * @param unsignedArtifact   the unsigned artifact to upload
     * @throws PipelineConnectorFacadeCallException occurs if the upload fails
     */
    void uploadUnsignedArtifact(UUID organizationId, UUID signingRequestId, String artifactUploadLink, File unsignedArtifact) throws PipelineConnectorFacadeCallException;

    /**
     * Waits for a signing request to reach a final status without downloading the artifact.
     *
     * @param organizationId   the organization ID where the signing request resides
     * @param signingRequestId the signing request ID as returned by submitSigningRequest
     * @throws PipelineConnectorFacadeCallException occurs if the request fails or times out
     */
    void waitForFinalSigningRequestStatus(UUID organizationId, UUID signingRequestId) throws PipelineConnectorFacadeCallException;

    /**
     * Downloads a signed artifact from the SignPath Pipeline Connector
     *
     * @param organizationId   the organization ID where the signing request resides
     * @param signingRequestID the signing request ID as returned by submitSigningRequest
     * @return the signed artifact in form of a TemporaryFile
     * @throws IOException                 occurs if any necessary intermediate file cannot be successfully created
     * @throws PipelineConnectorFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, PipelineConnectorFacadeCallException;
}
