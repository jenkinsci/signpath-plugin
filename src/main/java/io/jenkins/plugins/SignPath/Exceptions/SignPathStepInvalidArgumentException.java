package io.jenkins.plugins.SignPath.Exceptions;

/**
 * Occurs when user-provided arguments are invalid or missing in the
 *
 * @see io.jenkins.plugins.SignPath.SignPathStepBase
 */
public class SignPathStepInvalidArgumentException extends Exception {
    public SignPathStepInvalidArgumentException(String message) {
        super(message);
    }
}

