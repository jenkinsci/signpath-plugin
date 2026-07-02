package io.jenkins.plugins.signpath.ApiIntegration;

import io.jenkins.plugins.signpath.ApiIntegration.Model.ConnectorSigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitResult;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;

import java.io.IOException;
import java.util.UUID;

/**
 * A facade for the SignPath Pipeline Connector
 */
public interface SignPathFacade {

    /**
     * Submits a signing request to the SignPath Pipeline Connector. The connector pulls the artifact and its
     * SHA-256 sidecar from the Jenkins build's archived artifacts and forwards the request to the SignPath Application.
     *
     * @param submitModel all the (non-authentication) parameters including job/build identity and slugs
     * @return the signing request ID and web link
     * @throws SignPathFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    SubmitResult submitSigningRequest(ConnectorSigningRequestModel submitModel) throws SignPathFacadeCallException;

    /**
     * Waits for a signing request to reach a final status without downloading the artifact.
     *
     * @param organizationId   the organization ID where the signing request resides
     * @param signingRequestId the signing request ID as returned by submitSigningRequest
     * @throws SignPathFacadeCallException occurs if the request fails or times out
     */
    void waitForFinalSigningRequestStatus(UUID organizationId, UUID signingRequestId) throws SignPathFacadeCallException;

    /**
     * Downloads a signed artifact from the SignPath Pipeline Connector
     *
     * @param organizationId   the organization ID where the signing request resides
     * @param signingRequestID the signing request ID as returned by submitSigningRequest
     * @return the signed artifact in form of a TemporaryFile
     * @throws IOException                 occurs if any necessary intermediate file cannot be successfully created
     * @throws SignPathFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException;
}
