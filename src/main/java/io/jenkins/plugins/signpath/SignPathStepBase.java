package io.jenkins.plugins.signpath;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.Common.PluginConstants;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;
import jenkins.model.GlobalConfiguration;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.model.TaskListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A common base class for all SignPath API / Facade related Jenkins Steps
 * It encapsulates all required configuration related parameters
 * and helps share and re-use them across multiple steps
 * @see io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration
 */
public abstract class SignPathStepBase extends Step {
    // we set some sensible defaults for various timeouts, note that the
    // serviceUnavailableTimeoutInSeconds is used for upload, download and wait operations
    private int serviceUnavailableTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(10);
    private int uploadAndDownloadRequestTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(5);
    private int waitForCompletionTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(10);
    private int waitBetweenReadinessChecksInSeconds = (int) TimeUnit.SECONDS.toSeconds(30);

    // we set a sane default for the PowerShell call - 30min is pretty long for most customers already
    // if the customer ever runs into the problem that it is too short he will clearly see the exception
    // and can raise it accordingly (or improve his infrastructure to reduce necessary timeouts)
    // we explicitly decided together with PSA that it is not helpful to automatically calculate this overall timeout
    // because it is very hard to say what the correct overall timeout is (due to multiple factors playing a role)
    // also a timeout that is too high is not useful anymore
    private int waitForPowerShellTimeoutInSeconds = (int) TimeUnit.MINUTES.toSeconds(30);

    @Deprecated
    private String apiUrl = PluginConstants.DEFAULT_API_URL;
    @Deprecated
    private String trustedBuildSystemTokenCredentialId = PluginConstants.DEFAULT_TBS_TOKEN_CREDENTIAL_ID;
    private String apiTokenCredentialId = PluginConstants.DEFAULT_API_TOKEN_CREDENTIAL_ID;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiUrlWithGlobal() throws SignPathStepInvalidArgumentException {
        return getWithGlobalConfig(
            apiUrl,
            SignPathPluginGlobalConfiguration::getApiURL,
            "apiUrl");
    }

    public String getTrustedBuildSystemTokenCredentialId() {
        return trustedBuildSystemTokenCredentialId;
    }

    public String getTrustedBuildSystemTokenCredentialIdWithGlobal() throws SignPathStepInvalidArgumentException {
        return getWithGlobalConfig(
            trustedBuildSystemTokenCredentialId,
            SignPathPluginGlobalConfiguration::getTrustedBuildSystemCredentialId,
            "trustedBuildSystemTokenCredentialId");
    }

    public String getApiTokenCredentialId() {
        return apiTokenCredentialId;
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
    
    public int getWaitBetweenReadinessChecksInSeconds() {
        return waitBetweenReadinessChecksInSeconds;
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
    public void setApiTokenCredentialId(String apiTokenCredentialId) {
        this.apiTokenCredentialId = apiTokenCredentialId;
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
                getWaitForPowerShellTimeoutInSeconds(),
                getWaitBetweenReadinessChecksInSeconds());
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

    protected String getWithGlobalConfig(String stepLevelValue, Function<SignPathPluginGlobalConfiguration, String> globalValueGetter, String paramName) throws SignPathStepInvalidArgumentException {

        SignPathPluginGlobalConfiguration config = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        if (config == null) {
            throw new IllegalStateException("SignPathPluginGlobalConfiguration is not available. Ensure it is properly configured.");
        }
        String globalVal = globalValueGetter.apply(config);

        if (globalVal == null || globalVal.isEmpty()) {
            // global value is not set yet, fallback to the obsolete step-level value
            return stepLevelValue;
        }

        if (stepLevelValue == null || stepLevelValue.isEmpty()) {
            // step level value is not set, use global value
            return globalVal;
        }

        // here we have both values set. We enforce a rule that step level value should be identical to global value
        if (!stepLevelValue.equals(globalVal)) {
            throw new SignPathStepInvalidArgumentException(
                String.format(
                    "Parameter '%s' is configured globally to '%s' and cannot be changed in pipeline to '%s'.", 
                    paramName, globalVal, stepLevelValue));
        }

        // here it doesn't matter which value we return, as they are identical
        return stepLevelValue;
    }

    protected URL ensureValidURL(String apiUrl) throws SignPathStepInvalidArgumentException {
        try {
            return new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new SignPathStepInvalidArgumentException(apiUrl + " must be a valid url");
        }
    }
    
    protected SignPathPluginGlobalConfiguration getSignPathConfig() {
        SignPathPluginGlobalConfiguration config = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        if (config == null) {
            throw new IllegalStateException("SignPathPluginGlobalConfiguration is not available. Ensure it is properly configured.");
        }
        return config;
    }
}
