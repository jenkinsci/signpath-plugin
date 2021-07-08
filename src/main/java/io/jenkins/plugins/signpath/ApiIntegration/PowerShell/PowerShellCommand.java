package io.jenkins.plugins.signpath.ApiIntegration.PowerShell;

import hudson.util.Secret;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a safe version of a PowerShell command.
 * To avoid string injection attacks the command should not contain any values provided by the user.
 * Instead it should reference the environment variables via $env:variableName.
 */
public class PowerShellCommand {
    private final String command;
    private final Map<String, String> environmentVariables;
    private final Map<String, Secret> secretEnvironmentVariables;

    public PowerShellCommand(String command){
        this(command, new HashMap<>(), new HashMap<>());
    }

    public PowerShellCommand(String command, Map<String, String> environmentVariables, Map<String, Secret> secretEnvironmentVariables){
        this.command = command;
        this.environmentVariables = environmentVariables;
        this.secretEnvironmentVariables = secretEnvironmentVariables;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getEnvironmentVariables() {
        Map<String, String> result = new HashMap<>();

        result.putAll(environmentVariables);

        for(Map.Entry<String, Secret> entry : secretEnvironmentVariables.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getPlainText());
        }

        return result;
    }
}
