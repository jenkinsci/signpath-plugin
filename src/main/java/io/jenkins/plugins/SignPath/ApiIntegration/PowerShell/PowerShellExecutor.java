package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

/**
 * A utility class that helps us execute powershell commands
 * This is necessary for talking to the SignPath-API via our already existing PowerShell Module.
 */
public interface PowerShellExecutor {
    /**
     * Executes the given powershell command
     *
     * @param powerShellCommand the command to execute
     * @param timeoutInSeconds the maximum time before the command will be aborted
     * @return a @see PowerShellExecutionResult indicating whether the command has succeeded or failed
     */
    PowerShellExecutionResult execute(String powerShellCommand, int timeoutInSeconds);
}