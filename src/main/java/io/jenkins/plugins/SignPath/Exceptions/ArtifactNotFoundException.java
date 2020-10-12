package io.jenkins.plugins.SignPath.Exceptions;

public class ArtifactNotFoundException extends Exception {
    public ArtifactNotFoundException(String message){
        super(message);
    }
}
