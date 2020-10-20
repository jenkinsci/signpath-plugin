package io.jenkins.plugins.SignPath.Exceptions;

/**
 * Thrown when the
 *
 * @see io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager
 * does not find a given artifact
 */
public class ArtifactNotFoundException extends Exception {
    public ArtifactNotFoundException(String message) {
        super(message);
    }
}
