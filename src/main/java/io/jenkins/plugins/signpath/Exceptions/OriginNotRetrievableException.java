package io.jenkins.plugins.signpath.Exceptions;

import io.jenkins.plugins.signpath.OriginRetrieval.GitOriginRetriever;

/**
 * Thrown when the
 *
 * @see GitOriginRetriever
 * cannot retrieve the origin data from the build
 */
public class OriginNotRetrievableException extends Exception {
    public OriginNotRetrievableException(String message) {
        super(message);
    }
}
