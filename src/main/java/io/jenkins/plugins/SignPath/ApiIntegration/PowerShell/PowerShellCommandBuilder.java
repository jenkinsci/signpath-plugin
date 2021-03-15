package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import java.util.ArrayList;
import java.util.Arrays;
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
    private ArrayList<EnvironmentVariable> environmentVariables;

    public PowerShellCommandBuilder(String command){
        this.commandBuilder = new StringBuilder(command);
        this.environmentVariables = new ArrayList<>();
    }

    void appendFlag(String name){
        commandBuilder.append(String.format(" -%s", name));
    }

    void appendParameter(String name, String value){
        commandBuilder.append(String.format(" -%s '$($env:%s)'", name, name));
        environmentVariables.add(new EnvironmentVariable(name, value));
    }

    void appendCustom(String commandString, EnvironmentVariable... variables) {
        commandBuilder.append(commandString);
        environmentVariables.addAll(Arrays.stream(variables).collect(Collectors.toCollection(ArrayList::new)));
    }

    PowerShellCommand build(){
        EnvironmentVariable[] environmentVariableArray = new EnvironmentVariable[environmentVariables.size()];
        environmentVariables.toArray(environmentVariableArray);

        return new PowerShellCommand(commandBuilder.toString(), environmentVariableArray);
    }
}
