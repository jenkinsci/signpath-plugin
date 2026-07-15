package io.jenkins.plugins.signpath.ApiIntegration.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Result of submitting a signing request to the SignPath Pipeline Connector.
 */
@Getter
@Setter
@AllArgsConstructor
public class SubmitSigningRequestResult {
    private final String signingRequestId;
    private final String webLink;
}
