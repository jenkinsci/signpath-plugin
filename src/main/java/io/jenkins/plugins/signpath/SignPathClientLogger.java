package io.jenkins.plugins.signpath;

import java.io.PrintStream;

import io.signpath.signpathclient.SignPathClientSimpleLogger;

public class SignPathClientLogger implements SignPathClientSimpleLogger {
    private final PrintStream printStream;

    public SignPathClientLogger(PrintStream printStream) {
        this.printStream = printStream;
    }

    @Override
    public void log(String message, Throwable ex) {
        printStream.println(message);
        if(ex != null) {
            ex.printStackTrace(this.printStream);
        }
    }

    @Override
    public void log(String message) {
        printStream.println(message);
    }
}
