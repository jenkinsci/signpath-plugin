package io.jenkins.plugins.signpath.Exceptions;

/**
 * Occurs when a
 *
 * @see io.jenkins.plugins.signpath.SignPathStepBase
 * fails. See inner exception for more details.
 */
public class SignPathStepFailedException extends Exception {
    public SignPathStepFailedException(String message, Exception innerException) {
        super(message, innerException);
    }
}

