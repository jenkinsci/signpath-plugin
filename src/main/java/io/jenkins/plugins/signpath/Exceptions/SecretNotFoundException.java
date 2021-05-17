package io.jenkins.plugins.signpath.Exceptions;

import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;

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
