package io.jenkins.plugins.SignPath.Exceptions;

public class SignPathStepFailedException extends Exception {
    public SignPathStepFailedException(String message, Exception innerException){
        super(message, innerException);
    }
}

