package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

public class ApiConfigurationTest {
    @Test
    public void ctor_withValidTimeouts_doesNotThrow() throws MalformedURLException, SignPathStepInvalidArgumentException {
        int serviceUnavailableTimeoutInSeconds = 1;
        int uploadAndDownloadRequestTimeoutInSeconds = 2;
        int waitForCompletionTimeoutInSeconds = 3;
        int waitForPowerShellTimeoutInSeconds = 7;

        // ACT & ASSERT
        new ApiConfiguration(new URL(Some.url()),
                serviceUnavailableTimeoutInSeconds, uploadAndDownloadRequestTimeoutInSeconds, waitForCompletionTimeoutInSeconds, waitForPowerShellTimeoutInSeconds);
    }

    @Test
    public void ctor_withInvalidTimeouts_throws() {
        int serviceUnavailableTimeoutInSeconds = 1;
        int uploadAndDownloadRequestTimeoutInSeconds = 2;
        int waitForCompletionTimeoutInSeconds = 3;
        int waitForPowerShellTimeoutInSeconds = 6;

        // ACT
        ThrowingRunnable act = () -> new ApiConfiguration(new URL(Some.url()), serviceUnavailableTimeoutInSeconds, uploadAndDownloadRequestTimeoutInSeconds, waitForCompletionTimeoutInSeconds, waitForPowerShellTimeoutInSeconds);

        // ASSERT
        Throwable ex = assertThrows(SignPathStepInvalidArgumentException.class, act);

        assertEquals("The 'waitForPowerShellTimeoutInSeconds' (6) must be " +
                "greater than the other tree timeouts combined (6)", ex.getMessage());
    }
}
