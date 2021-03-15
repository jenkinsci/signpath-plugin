package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

public class PowerShellCommand {
    private final String command;
    private final EnvironmentVariable[] environmentVariables;

    public PowerShellCommand(String command, EnvironmentVariable... environmentVariables){

        this.command = command;
        this.environmentVariables = environmentVariables;
    }

    public String getCommand() {
        return command;
    }

    public EnvironmentVariable[] getEnvironmentVariables() {
        return environmentVariables;
    }
}
