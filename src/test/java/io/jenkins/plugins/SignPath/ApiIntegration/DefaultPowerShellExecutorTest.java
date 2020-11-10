package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.DefaultPowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import org.junit.Before;
import org.junit.Test;

import static io.jenkins.plugins.SignPath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;

public class DefaultPowerShellExecutorTest {
    private DefaultPowerShellExecutor sut;

    @Before
    public void setup() {
        sut = new DefaultPowerShellExecutor("pwsh");
    }

    @Test
    public void execute() {
        PowerShellExecutionResult executionResult = sut.execute("echo 'some string'", Integer.MAX_VALUE);

        assertContains("some string", executionResult.getOutput());
        assertFalse(executionResult.getHasError());
    }

    @Test
    public void execute_withError() {
        PowerShellExecutionResult executionResult = sut.execute("echo 'some string'; exit 1", Integer.MAX_VALUE);

        assertContains("some string", executionResult.getOutput());
        assertTrue(executionResult.getHasError());
    }

    @Test
    public void execute_withTimeout() {
        PowerShellExecutionResult executionResult = sut.execute("Start-Sleep -Seconds 5;", 0);

        assertEquals("Execution did not complete within 0s", executionResult.getOutput());
        assertTrue(executionResult.getHasError());
    }
}
