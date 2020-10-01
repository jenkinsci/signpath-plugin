package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;

import java.util.HashMap;
import java.util.Map;

public class PowerShellExecutor implements IPowerShellExecutor {

    private final String powerShellExecutableName;

    public PowerShellExecutor(String powerShellExecutableName){

        this.powerShellExecutableName = powerShellExecutableName;
    }

    public PowerShellExecutionResult execute(String powerShellCommand){
        try (PowerShell powerShell = PowerShell.openSession(powerShellExecutableName)) {
            powerShell.executeCommand("Import-Module C:\\Development\\signpath.application\\src\\Applications.Api\\wwwroot\\Tools\\SignPath.psm1");
            Map<String, String> config = new HashMap<>();
            config.put("maxWait", "500000"); // wait 500s...
            powerShell.configuration(config);
            PowerShellResponse response = powerShell.executeCommand(powerShellCommand);
            return new PowerShellExecutionResult(response.isError(), response.getCommandOutput());
        }
    }
}
