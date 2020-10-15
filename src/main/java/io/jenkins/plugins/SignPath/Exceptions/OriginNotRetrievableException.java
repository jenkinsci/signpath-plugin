package io.jenkins.plugins.SignPath.Exceptions;

import io.jenkins.plugins.SignPath.OriginRetrieval.GitOriginRetriever;

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
