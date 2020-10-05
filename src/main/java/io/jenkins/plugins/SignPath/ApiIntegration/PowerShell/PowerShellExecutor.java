package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;

public class PowerShellExecutor implements IPowerShellExecutor {

    private final String powerShellExecutableName;
    private String setupEnvironmentCommand;

    public PowerShellExecutor(String powerShellExecutableName, String setupEnvironmentCommand) {
        this.powerShellExecutableName = powerShellExecutableName;
        this.setupEnvironmentCommand = setupEnvironmentCommand;
    }

    public PowerShellExecutionResult execute(String powerShellCommand) {
        try {
            Process powerShellProcess = Runtime.getRuntime().exec(String.format("%s -command \"%s;%s\"", powerShellExecutableName, setupEnvironmentCommand, powerShellCommand));
            powerShellProcess.getOutputStream().close();
            String standard = IOUtils.toString(powerShellProcess.getInputStream(), StandardCharsets.UTF_8).trim();
            String error = IOUtils.toString(powerShellProcess.getErrorStream(), StandardCharsets.UTF_8).trim();
            powerShellProcess.destroy();
            int exitValue = powerShellProcess.exitValue();
            if(exitValue!=0 || !error.isEmpty()){
                return new PowerShellExecutionResult(true, standard + error);
            }

            return new PowerShellExecutionResult(false, standard + error);
        } catch (Exception e) {
            return new PowerShellExecutionResult(true, e.toString());
        }
    }
}
