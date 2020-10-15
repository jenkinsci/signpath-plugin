package io.jenkins.plugins.SignPath.Exceptions;

/**
 * Occurs when the
 *
 * @see io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacade
 * failed to call SignPath
 */
public class SignPathFacadeCallException extends Exception {
    public SignPathFacadeCallException(String message) {
        super(message);
    }
}

