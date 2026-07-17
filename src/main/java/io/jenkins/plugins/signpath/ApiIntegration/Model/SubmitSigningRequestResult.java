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
}
