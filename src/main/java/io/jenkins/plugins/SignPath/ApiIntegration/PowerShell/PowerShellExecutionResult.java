package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

public class PowerShellExecutionResult {
    private Boolean hasError;
    private String output;

    public PowerShellExecutionResult(Boolean hasError, String output) {
        this.hasError = hasError;
        this.output = output;
    }

    public Boolean getHasError() {
        return hasError;
    }

    public String getOutput() {
        return output;
    }
}
