package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

/**
 * Holds an environment variable that is passed to our PowerShell Script
 * We use environment variables as a way to prevent string-injection attacks into our PowerShell Script Call
 */
public class EnvironmentVariable {

    private final String name;
    private final String value;

    public EnvironmentVariable(String name, String value){

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
