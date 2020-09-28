package io.jenkins.plugins.SignPath.Exceptions;

public class SecretNotFoundException extends Exception {
    public SecretNotFoundException(String message){
        super(message);
    }
}
