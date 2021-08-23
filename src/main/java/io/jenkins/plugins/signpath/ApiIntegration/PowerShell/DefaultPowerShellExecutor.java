package io.jenkins.plugins.signpath.ApiIntegration.PowerShell;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class DefaultPowerShellExecutor implements PowerShellExecutor {

    private final String powerShellExecutableName;
    private final PrintStream logger;

    public DefaultPowerShellExecutor(String powerShellExecutableName, PrintStream logger) {
        this.powerShellExecutableName = powerShellExecutableName;
        this.logger = logger;
    }

    public PowerShellExecutionResult execute(PowerShellCommand powerShellCommand, int timeoutInSeconds) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(powerShellExecutableName, "-command", powerShellCommand.getCommand());
            processBuilder.environment().putAll(powerShellCommand.getEnvironmentVariables());

            // redirect error stream to output stream to enable us to get one combined string of both stdout & stderr
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            LineReader lineReader = new LineReader(process.getInputStream(), logger);
            Thread outputReaderThread = new Thread(lineReader);
            outputReaderThread.start();

            boolean hasCompletedWithoutTimeout = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);

            if (!hasCompletedWithoutTimeout) {
                return PowerShellExecutionResult.error(String.format("Execution did not complete within %ds", timeoutInSeconds));
            }

            int exitValue = process.exitValue();

            outputReaderThread.join();
            process.destroy();

            if (exitValue != 0) {
                return PowerShellExecutionResult.error(String.format("Execution did not complete successfully (ExitCode: %d)", exitValue));
            }

            return PowerShellExecutionResult.success(lineReader.getCapturedOutput());
        } catch (Exception e) {
            return PowerShellExecutionResult.error(e.toString());
        }
    }

    private static class LineReader implements Runnable {
        private final InputStream input;
        private final PrintStream output;
        private final StringBuilder stringBuilder;

        public LineReader(InputStream input, PrintStream output) {
            this.input = input;
            this.output = output;
            this.stringBuilder = new StringBuilder();
        }

        public String getCapturedOutput() {
            return stringBuilder.toString();
        }

        @Override
        public void run() {
            String lineSeparator = System.getProperty("line.separator");

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while((line = reader.readLine()) != null) {
                    output.println(line);
                    stringBuilder.append(line);
                    stringBuilder.append(lineSeparator);
                }
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}