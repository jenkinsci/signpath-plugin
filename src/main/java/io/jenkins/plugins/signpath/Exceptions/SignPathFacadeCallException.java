package io.jenkins.plugins.signpath.Exceptions;

/**
 * Occurs when the
 *
 * @see io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade
 * failed to call SignPath
 */
public class SignPathFacadeCallException extends Exception {
    public SignPathFacadeCallException(String message) {
        super(message);
    }
}

