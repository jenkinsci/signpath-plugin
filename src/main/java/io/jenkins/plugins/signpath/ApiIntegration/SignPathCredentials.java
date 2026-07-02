package io.jenkins.plugins.signpath.ApiIntegration;

import hudson.util.Secret;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Holds all necessary credentials to authenticate against the SignPath Pipeline Connector endpoints, exposed by the
 *
 * @see PipelineConnectorFacade
 */
@Getter
@AllArgsConstructor
public class SignPathCredentials {
    private final Secret apiToken;
}
