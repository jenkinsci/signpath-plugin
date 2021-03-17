package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import java.util.Map;

/**
 * Represents a safe version of a PowerShell command
 * To avoid string injection attacks the command should not contain any values provided by the user
 * Instead it should reference the environment variables via $env:variableName
 */
public class PowerShellCommand {
    private final String command;
    private final Map<String, String> environmentVariables;

    public PowerShellCommand(String command, Map<String, String> environmentVariables){

        this.command = command;
        this.environmentVariables = environmentVariables;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
}
