package io.jenkins.plugins.signpath.ApiIntegration;

import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.DefaultPowerShellExecutor;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellCommand;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.TestUtils.PortablePowerShell;
import io.jenkins.plugins.signpath.TestUtils.PortablePowerShellRule;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.zip.ZipEntry;
import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import static io.jenkins.plugins.signpath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;

public class DefaultPowerShellExecutorTest {
    private ByteArrayOutputStream outputStream;

    private DefaultPowerShellExecutor sut;

    @ClassRule
    public static PortablePowerShellRule ps = new PortablePowerShellRule(false);

    @Before
    public void setup() {
        outputStream = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(outputStream);

        sut = new DefaultPowerShellExecutor(ps.getPowerShellExecutable(), logger);
    }

    @Test
    public void execute() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("echo 'some string'"), Integer.MAX_VALUE);

        // ASSERT
        assertFalse(executionResult.getHasError());

        assertNotNull(executionResult.getOutput());
        assertContains("some string", executionResult.getOutput());
        assertContains("some string", outputStream.toString());
    }

    @Test
    public void execute_withEnvironmentVariables() {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("variable", "content");

        Map<String, Secret> secretEnvironmentVariables = new HashMap<>();
        secretEnvironmentVariables.put("secret", Secret.fromString("secretcontent"));

        // ACT
        PowerShellExecutionResult executionResult =
                sut.execute(new PowerShellCommand("echo $env:variable $env:secret", environmentVariables, secretEnvironmentVariables), Integer.MAX_VALUE);

        // ASSERT
        assertFalse(executionResult.getHasError());

        assertNotNull(executionResult.getOutput());
        assertContains("content", executionResult.getOutput());
        assertContains("content", outputStream.toString());

        assertContains("secretcontent", executionResult.getOutput());
        assertContains("secretcontent", outputStream.toString());
    }

    @Test
    public void execute_withError() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("echo 'some string'; exit 1"), Integer.MAX_VALUE);

        // ASSERT
        assertTrue(executionResult.getHasError());
        assertEquals("Execution did not complete successfully (ExitCode: 1)", executionResult.getErrorDescription());
        assertNull(executionResult.getOutput());

        assertContains("some string", outputStream.toString());
    }

    @Test
    public void execute_withTimeout() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("Start-Sleep -Seconds 5;"), 0);

        // ASSERT
        assertTrue(executionResult.getHasError());
        assertEquals("Execution did not complete within 0s", executionResult.getErrorDescription());
        assertNull(executionResult.getOutput());

        // cannot assert outputStream here since the timeout is immediately reached before any output is captured
    }
}
