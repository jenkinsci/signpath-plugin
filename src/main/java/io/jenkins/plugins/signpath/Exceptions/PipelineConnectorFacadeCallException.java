package io.jenkins.plugins.signpath.Exceptions;

import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacade;

/**
 * Occurs when the
 *
 * @see IPipelineConnectorFacade
 * failed to call the SignPath Pipeline Connector
 */
public class PipelineConnectorFacadeCallException extends Exception {
    public PipelineConnectorFacadeCallException(String message) {
        super(message);
    }
}

