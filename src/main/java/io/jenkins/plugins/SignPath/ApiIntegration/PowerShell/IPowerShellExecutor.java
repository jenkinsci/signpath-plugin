package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

public interface IPowerShellExecutor {
    PowerShellExecutionResult execute(String powerShellCommand);
}
