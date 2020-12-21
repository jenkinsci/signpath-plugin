package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import javax.annotation.Nullable;

/**
 * Indicates whether a given execution via
 *
 * @see PowerShellExecutor
 * has been successful or not.
 */
public class PowerShellExecutionResult {
    private final boolean hasError;
    private final String errorDescription;
    private final String output;

    private PowerShellExecutionResult(boolean hasError, String errorDescription, String output) {
        this.hasError = hasError;
        this.errorDescription = errorDescription;
        this.output = output;
    }

    public static PowerShellExecutionResult Success(String output) {
        return new PowerShellExecutionResult(false, null, output);
    }

    public static PowerShellExecutionResult Error(String errorDescription) {
        return new PowerShellExecutionResult(true, errorDescription, null);
    }

    public boolean getHasError() {
        return hasError;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    @Nullable
    public String getOutput() {
        return output;
    }
}