package io.jenkins.plugins.signpath;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A common base class for all SignPath API / Facade related Jenkins Steps
 * It encapsulates all required configuration related parameters
 * and helps share and re-use them across multiple steps
 * @see io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration
 */
public abstract class SignPathStepBase extends Step {
    private String apiUrl = "https://app.signpath.io/api/";

    // we set some sensible defaults for various timeouts, note that the
    // serviceUnavailableTimeoutInSeconds is used for upload, download and wait operations
    private int serviceUnavailableTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(10);
    private int uploadAndDownloadRequestTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(5);
    private int waitForCompletionTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(10);

    // we set a sane default for the PowerShell call - 30min is pretty long for most customers already
    // if the customer ever runs into the problem that it is too short he will clearly see the exception
    // and can raise it accordingly (or improve his infrastructure to reduce necessary timeouts)
    // we explicitly decided together with PSA that it is not helpful to automatically calculate this overall timeout
    // because it is very hard to say what the correct overall timeout is (due to multiple factors playing a role)
    // also a timeout that is too high is not useful anymore
    private int waitForPowerShellTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(30);

    private String trustedBuildSystemTokenCredentialId = "SignPath.TrustedBuildSystemToken";
    private String ciUserTokenCredentialId = "SignPath.CIUserToken";

    public String getApiUrl() {
        return apiUrl;
    }

    public String getTrustedBuildSystemTokenCredentialId() {
        return trustedBuildSystemTokenCredentialId;
    }

    public String getCiUserTokenCredentialId() {
        return ciUserTokenCredentialId;
    }

    public int getServiceUnavailableTimeoutInSeconds() {
        return serviceUnavailableTimeoutInSeconds;
    }

    public int getUploadAndDownloadRequestTimeoutInSeconds() {
        return uploadAndDownloadRequestTimeoutInSeconds;
    }

    public int getWaitForCompletionTimeoutInSeconds() {
        return waitForCompletionTimeoutInSeconds;
    }

    public int getWaitForPowerShellTimeoutInSeconds() {
        return waitForPowerShellTimeoutInSeconds;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @DataBoundSetter
    public void setTrustedBuildSystemTokenCredentialId(String trustedBuildSystemTokenCredentialId) {
        this.trustedBuildSystemTokenCredentialId = trustedBuildSystemTokenCredentialId;
    }

    @DataBoundSetter
    public void setCiUserTokenCredentialId(String ciUserTokenCredentialId) {
        this.ciUserTokenCredentialId = ciUserTokenCredentialId;
    }

    @DataBoundSetter
    public void setServiceUnavailableTimeoutInSeconds(int serviceUnavailableTimeoutInSeconds) {
        this.serviceUnavailableTimeoutInSeconds = serviceUnavailableTimeoutInSeconds;
    }

    @DataBoundSetter
    public void setUploadAndDownloadRequestTimeoutInSeconds(int uploadAndDownloadRequestTimeoutInSeconds) {
        this.uploadAndDownloadRequestTimeoutInSeconds = uploadAndDownloadRequestTimeoutInSeconds;
    }

    @DataBoundSetter
    public void setWaitForCompletionTimeoutInSeconds(int waitForCompletionTimeoutInSeconds) {
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
    }

    @DataBoundSetter
    public void setWaitForPowerShellTimeoutInSeconds(int waitForPowerShellTimeoutInSeconds) {
        this.waitForPowerShellTimeoutInSeconds = waitForPowerShellTimeoutInSeconds;
    }

    public ApiConfiguration getAndValidateApiConfiguration() throws SignPathStepInvalidArgumentException {
        return new ApiConfiguration(
                ensureValidURL(getApiUrl()),
                getServiceUnavailableTimeoutInSeconds(),
                getUploadAndDownloadRequestTimeoutInSeconds(),
                getWaitForCompletionTimeoutInSeconds(),
                getWaitForPowerShellTimeoutInSeconds());
    }

    protected URL ensureValidURL(String apiUrl) throws SignPathStepInvalidArgumentException {
        try {
            return new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new SignPathStepInvalidArgumentException(apiUrl + " must be a valid url");
        }
    }

    protected UUID ensureValidUUID(String input, String name) throws SignPathStepInvalidArgumentException {
        try {
            return UUID.fromString(ensureNotNull(input, name));
        } catch (IllegalArgumentException ex) {
            throw new SignPathStepInvalidArgumentException(name + " must be a valid uuid");
        }
    }

    protected String ensureNotNull(String input, String name) throws SignPathStepInvalidArgumentException {
        if (input == null)
            throw new SignPathStepInvalidArgumentException(name + " must be set");

        return input;
    }
}
