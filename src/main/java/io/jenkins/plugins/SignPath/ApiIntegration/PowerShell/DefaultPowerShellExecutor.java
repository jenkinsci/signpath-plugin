package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DefaultPowerShellExecutor implements PowerShellExecutor {

    private final String powerShellExecutableName;
    private final PrintStream logger;

    public DefaultPowerShellExecutor(String powerShellExecutableName, PrintStream logger) {
        this.powerShellExecutableName = powerShellExecutableName;
        this.logger = logger;
    }

    public PowerShellExecutionResult execute(String powerShellCommand,
                                             int timeoutInSeconds,
                                             EnvironmentVariable... environmentVariables) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(powerShellExecutableName, "-command", powerShellCommand);

            Map<String, String> environmentMap =
                    Arrays.stream(environmentVariables).collect(Collectors.toMap(e -> e.getName(), p -> p.getValue()));

            processBuilder.environment().putAll(environmentMap);

            // redirect error stream to output stream to enable us to get one combined string of both stdout & stderr
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            LineReader lineReader = new LineReader(process.getInputStream(), logger);
            Thread outputReaderThread = new Thread(lineReader);
            outputReaderThread.start();

            boolean hasCompletedWithoutTimeout = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);

            if (!hasCompletedWithoutTimeout) {
                return PowerShellExecutionResult.Error(String.format("Execution did not complete within %ds", timeoutInSeconds));
            }

            int exitValue = process.exitValue();

            outputReaderThread.join();
            process.destroy();

            if (exitValue != 0) {
                return PowerShellExecutionResult.Error(String.format("Execution did not complete successfully (ExitCode: %d)", exitValue));
            }

            return PowerShellExecutionResult.Success(lineReader.getCapturedOutput());
        } catch (Exception e) {
            return PowerShellExecutionResult.Error(e.toString());
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

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
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