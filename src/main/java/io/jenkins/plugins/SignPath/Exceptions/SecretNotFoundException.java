package io.jenkins.plugins.SignPath.Exceptions;

import io.jenkins.plugins.SignPath.SecretRetrieval.SecretRetriever;

/**
 * Occurs when the
 *
 * @see SecretRetriever
 * cannot find a given secret
 */
public class SecretNotFoundException extends Exception {
    public SecretNotFoundException(String message) {
        super(message);
    }
}
