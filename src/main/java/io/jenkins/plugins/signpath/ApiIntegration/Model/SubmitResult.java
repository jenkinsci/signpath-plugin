package io.jenkins.plugins.signpath.ApiIntegration.Model;

import java.util.UUID;

/**
 * Result of submitting a signing request to the SignPath Pipeline Connector.
 */
public class SubmitResult {
    private final UUID signingRequestId;
    private final String webLink;

    public SubmitResult(UUID signingRequestId, String webLink) {
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
