package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.UUID;

/**
 * Result returned by the SubmitWithArtifactRetrievalLink route.
 * Contains the signing request ID and the web link to view the request.
 * No upload link is present — SignPath retrieves the artifact from the provided URL directly.
 */
public class SubmitSigningRequestWithArtifactRetrievalLinkResult {
    private final UUID signingRequestId;
    private final String webLink;

    public SubmitSigningRequestWithArtifactRetrievalLinkResult(UUID signingRequestId, String webLink) {
        this.signingRequestId = signingRequestId;
        this.webLink = webLink;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    public String getWebLink() {
        return webLink;
    }
}
