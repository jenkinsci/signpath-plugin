package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;

public class PowerShellExecutor implements IPowerShellExecutor {

    private final String powerShellExecutableName;

    public PowerShellExecutor(String powerShellExecutableName){

        this.powerShellExecutableName = powerShellExecutableName;
    }

    public PowerShellExecutionResult execute(String powerShellCommand){
        try (PowerShell powerShell = PowerShell.openSession(powerShellExecutableName)) {
            PowerShellResponse response = powerShell.executeCommand(powerShellCommand);
            return new PowerShellExecutionResult(response.isError(), response.getCommandOutput());
        }
    }
}
