package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

/**
 * Indicates whether a given execution via
 *
 * @see PowerShellExecutor
 * has been successful or not.
 */
public class PowerShellExecutionResult {
    private final boolean hasError;
    private final String output;

    public PowerShellExecutionResult(boolean hasError, String output) {
        this.hasError = hasError;
        this.output = output;
    }

    public boolean getHasError() {
        return hasError;
    }

    public String getOutput() {
        return output;
    }
}