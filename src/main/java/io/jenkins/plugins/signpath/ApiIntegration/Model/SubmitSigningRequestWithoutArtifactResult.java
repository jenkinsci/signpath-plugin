package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.UUID;

/**
 * Result returned by the SubmitWithoutArtifact route.
 * Contains the signing request ID and the upload link to which the artifact must be uploaded.
 */
public class SubmitSigningRequestWithoutArtifactResult {
    private final UUID signingRequestId;
    private final String uploadLink;
    private final String webLink;

    public SubmitSigningRequestWithoutArtifactResult(UUID signingRequestId, String uploadLink, String webLink) {
        this.signingRequestId = signingRequestId;
        this.uploadLink = uploadLink;
        this.webLink = webLink;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    public String getUploadLink() {
        return uploadLink;
    }

    public String getWebLink() {
        return webLink;
    }
}
