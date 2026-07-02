package io.jenkins.plugins.signpath.Exceptions;

/**
 * Occurs when the
 *
 * @see io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacade
 * failed to call the SignPath Pipeline Connector
 */
public class SignPathFacadeCallException extends Exception {
    public SignPathFacadeCallException(String message) {
        super(message);
    }
}

