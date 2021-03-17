package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import com.github.fge.jsonschema.library.Dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * A utility class that helps in building a
 * @see PowerShellCommand
 * similar to
 * @see StringBuilder
 * to avoid string injection attacks
 */
public class PowerShellCommandBuilder {
    private StringBuilder commandBuilder;
    private Map<String, String> environmentVariables;

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
