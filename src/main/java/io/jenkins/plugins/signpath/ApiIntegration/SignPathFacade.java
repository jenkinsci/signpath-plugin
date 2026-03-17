package io.jenkins.plugins.signpath.ApiIntegration;

import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithArtifactRetrievalLinkModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithoutArtifactModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithArtifactRetrievalLinkResult;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithoutArtifactResult;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * A facade for the SignPath API
 */
public interface SignPathFacade {

    /**
     * Submits a signing request without uploading the artifact to the Jenkins server.
     * The artifact SHA-256 hash is passed instead; the artifact is then uploaded
     * directly using the returned upload link.
     *
     * @param submitModel all the (non-authentication) parameters including filename and SHA-256 hex hash
     * @return the signing request ID, upload link, and web link
     * @throws SignPathFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    SubmitSigningRequestWithoutArtifactResult submitSigningRequestWithoutArtifact(SigningRequestWithoutArtifactModel submitModel) throws SignPathFacadeCallException;

    /**
     * Submits a signing request where SignPath will retrieve the artifact from a provided URL.
     * The artifact SHA-256 hash is passed to verify integrity after retrieval.
     * No artifact upload is performed by the caller.
     *
     * @param submitModel all the (non-authentication) parameters including filename, SHA-256 hex hash, retrieval URL, and optional HTTP headers
     * @return the signing request ID and web link
     * @throws SignPathFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    SubmitSigningRequestWithArtifactRetrievalLinkResult submitSigningRequestWithArtifactRetrievalLink(SigningRequestWithArtifactRetrievalLinkModel submitModel) throws SignPathFacadeCallException;

    /**
     * Uploads an unsigned artifact to SignPath using the upload link returned by submitSigningRequestWithoutArtifact.
     *
     * @param uploadLink     the upload URL as returned by the SubmitWithoutArtifact route
     * @param artifactStream the artifact content to upload
     * @throws IOException                 occurs if the artifact stream cannot be read or written to a temp file
     * @throws SignPathFacadeCallException occurs if the upload request fails
     */
    void uploadUnsignedArtifact(String uploadLink, InputStream artifactStream) throws IOException, SignPathFacadeCallException;

    /**
     * Downloads a signed artifact from SignPath
     *
     * @param organizationId   the organization ID where the signing request resides
     * @param signingRequestID the signing request ID as returned by submitSigningRequestWithoutArtifact
     * @return the signed artifact in form of a TemporaryFile
     * @throws IOException                 occurs if any necessary intermediate file cannot be successfully created
     * @throws SignPathFacadeCallException occurs if any user error has been made (i.e. misconfiguration)
     */
    TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) throws IOException, SignPathFacadeCallException;
}
