package io.jenkins.plugins.SignPath.Exceptions;

/**
 * Occurs when a
 *
 * @see io.jenkins.plugins.SignPath.SignPathStepBase
 * fails. See inner exception for more details.
 */
public class SignPathStepFailedException extends Exception {
    public SignPathStepFailedException(String message, Exception innerException) {
        super(message, innerException);
    }
}

