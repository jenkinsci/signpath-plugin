package io.jenkins.plugins.signpath.Exceptions;

/**
 * Occurs when user-provided arguments are invalid or missing in the
 *
 * @see io.jenkins.plugins.signpath.SignPathStepBase
 */
public class SignPathStepInvalidArgumentException extends Exception {
    public SignPathStepInvalidArgumentException(String message) {
        super(message);
    }
}

