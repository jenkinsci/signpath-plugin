package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class DefaultPowerShellExecutor implements PowerShellExecutor {

    private final String powerShellExecutableName;

    public DefaultPowerShellExecutor(String powerShellExecutableName) {
        this.powerShellExecutableName = powerShellExecutableName;
    }

    public PowerShellExecutionResult execute(String powerShellCommand, int timeoutInSeconds) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(powerShellExecutableName, "-command", powerShellCommand);

            try (TemporaryFile outputFile = new TemporaryFile()) {
                processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(outputFile.getFile()));
                processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile.getFile()));

                Process process = processBuilder.start();
                boolean hasCompletedWithoutTimeout = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);

                if (!hasCompletedWithoutTimeout) {
                    return new PowerShellExecutionResult(true, String.format("Execution did not complete within %ds", timeoutInSeconds));
                }

                int exitValue = process.exitValue();

                String output = String.join(System.lineSeparator(), Files.readAllLines(Paths.get(outputFile.getAbsolutePath())));

                if (exitValue != 0) {
                    return new PowerShellExecutionResult(true, output);
                }

                return new PowerShellExecutionResult(false, output);
            }
        } catch (Exception e) {
            return new PowerShellExecutionResult(true, e.toString());
        }
    }
}