package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import java.util.ArrayList;

public class PowerShellCommandBuilder {
    private StringBuilder commandBuilder;
    private ArrayList<EnvironmentVariable> environmentVariables;

    public PowerShellCommandBuilder(String command){
        this.commandBuilder = new StringBuilder(command);
        this.environmentVariables = new ArrayList<>();
    }

    void appendParameter(String name, String value){
        commandBuilder.append(String.format(" -%s '$($env:%s)'", name, name));
        environmentVariables.add(new EnvironmentVariable(name, value));
    }

    void appendFlag(String name){
        commandBuilder.append(String.format(" -%s", name));
    }

    PowerShellCommand build(){
        EnvironmentVariable[] environmentVariableArray = new EnvironmentVariable[environmentVariables.size()];
        environmentVariables.toArray(environmentVariableArray);

        return new PowerShellCommand(commandBuilder.toString(), environmentVariableArray);
    }
}
