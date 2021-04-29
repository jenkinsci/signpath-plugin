package io.jenkins.plugins.signpath.ApiIntegration.PowerShell;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that helps in building a
 * @see PowerShellCommand
 * similar to
 * @see StringBuilder
 * to avoid string injection attacks
 */
public class PowerShellCommandBuilder {
    private final StringBuilder commandBuilder;
    private final Map<String, String> environmentVariables;

    public PowerShellCommandBuilder(String command){
        this.commandBuilder = new StringBuilder(command);
        this.environmentVariables = new HashMap<>();
    }

    void appendFlag(String name){
        commandBuilder.append(String.format(" -%s", name));
    }

    void appendParameter(String name, String value){
        commandBuilder.append(String.format(" -%s \"$($env:%s)\"", name, name));
        environmentVariables.put(name, value);
    }

    void appendCustom(String commandString, Map<String, String> variables) {
        commandBuilder.append(String.format(" %s", commandString));
        environmentVariables.putAll(variables);
    }

    PowerShellCommand build(){
        return new PowerShellCommand(commandBuilder.toString(), environmentVariables);
    }
}
