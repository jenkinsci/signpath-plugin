package io.jenkins.plugins.signpath.Exceptions;

/**
 * Thrown when the
 *
 * @see io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager
 * does not find a given artifact
 */
public class ArtifactNotFoundException extends Exception {
    public ArtifactNotFoundException(String message) {
        super(message);
    }
}
