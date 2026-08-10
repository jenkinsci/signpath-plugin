package io.jenkins.plugins.signpath.ApiIntegration.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Result of submitting a signing request to the SignPath Pipeline Connector.
 */
@Getter
@Setter
@AllArgsConstructor
public class SubmitSigningRequestResult {
    private final UUID signingRequestId;
    private final String webLink;

    /**
     * The link that must be passed back to the connector when uploading the unsigned artifact.
     * It is only returned when the artifact is uploaded by the plugin (i.e. no input artifact retrieval URL was used).
     */
    private final String artifactUploadLink;
}
