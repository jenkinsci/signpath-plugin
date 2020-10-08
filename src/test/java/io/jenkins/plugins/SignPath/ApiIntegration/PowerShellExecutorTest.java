package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutor;
import org.junit.Before;
import org.junit.Test;

import static io.jenkins.plugins.SignPath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;

public class PowerShellExecutorTest {
    private PowerShellExecutor sut;

    @Before
    public void setup(){
        sut = new PowerShellExecutor("pwsh");
    }

    @Test
    public void execute(){
        PowerShellExecutionResult executionResult = sut.execute("echo 'some string'");

        assertContains("some string", executionResult.getOutput());
        assertFalse(executionResult.getHasError());
    }

    @Test
    public void execute_withError(){
        PowerShellExecutionResult executionResult = sut.execute("echo 'some string'; exit 1");

        assertContains("some string", executionResult.getOutput());
        assertTrue(executionResult.getHasError());
    }

    @Test
    public void execute_WithErrorText(){
        PowerShellExecutionResult executionResult = sut.execute("Write-Error 'fatal';");

        assertContains("fatal", executionResult.getOutput());
        assertTrue(executionResult.getHasError());
    }
}
