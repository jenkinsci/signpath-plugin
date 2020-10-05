package io.jenkins.plugins.SignPath;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.IPowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.SignPathPowerShellFacadeFactory;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Artifacts.IArtifactFileManager;
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
import io.jenkins.plugins.SignPath.StepInputParser.GetSignedArtifactStepInput;
import io.jenkins.plugins.SignPath.StepInputParser.SigningRequestStepInputParser;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Set;

public class GetSignedArtifactStep extends Step {
    private final static String FunctionName = "getSignedArtifact";
    private final static String DisplayName = "Download SignPath Signed Artifact";

    private String apiUrl =  "https://app.signpath.io/api/";
    private int serviceUnavailableTimeoutInSeconds = 600;
    private int uploadAndDownloadRequestTimeoutInSeconds = 300;
    private int waitForCompletionTimeoutInSeconds = 600;
    private String ciUserToken;

    private String organizationId;
    private String signingRequestId;
    private String outputArtifactPath;

    @DataBoundConstructor
    public GetSignedArtifactStep() {
    }

    // TODO SIGN-3326: Check all input parameters for null! (Write mapper?)
    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        GetSignedArtifactStepInput input = SigningRequestStepInputParser.Parse(this);

        TaskListener listener = context.get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = context.get(Run.class);
        Launcher launcher = context.get(Launcher.class);
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();

        // TODO SIGN-3326: Share between steps + validate configuration
        ISecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        IArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);
        String setupEnvironmentCommand = "Import-Module C:\\Development\\signpath.application\\src\\Applications.Api\\wwwroot\\Tools\\SignPath.psm1";
        IPowerShellExecutor pwsh = new PowerShellExecutor("pwsh", setupEnvironmentCommand);
        ApiConfiguration apiConfiguration = new ApiConfiguration(new URL(getApiUrl()),
                getServiceUnavailableTimeoutInSeconds(),
                getUploadAndDownloadRequestTimeoutInSeconds(),
                getWaitForCompletionTimeoutInSeconds());
        ISignPathFacadeFactory signPathFacadeFactory = new SignPathPowerShellFacadeFactory(pwsh, apiConfiguration);

        return new GetSignedArtifactStepExecution(input, context, logger, secretRetriever, artifactFileManager, signPathFacadeFactory);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(FilePath.class, Run.class, Launcher.class, TaskListener.class, EnvVars.class);
        }

        @Override
        public String getFunctionName() {
            return FunctionName;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return DisplayName;
        }
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getCiUserToken() {
        return ciUserToken;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getSigningRequestId() {
        return signingRequestId;
    }

    public int getServiceUnavailableTimeoutInSeconds() {
        return serviceUnavailableTimeoutInSeconds;
    }

    public int getUploadAndDownloadRequestTimeoutInSeconds() {
        return uploadAndDownloadRequestTimeoutInSeconds;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }

    public int getWaitForCompletionTimeoutInSeconds() {
        return waitForCompletionTimeoutInSeconds;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @DataBoundSetter
    public void setCiUserToken(String ciUserToken) {
        this.ciUserToken = ciUserToken;
    }

    @DataBoundSetter
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @DataBoundSetter
    public void setSigningRequestId(String signingRequestId) {
        this.signingRequestId = signingRequestId;
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
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }

    @DataBoundSetter
    public void setWaitForCompletionTimeoutInSeconds(int waitForCompletionTimeoutInSeconds) {
        this.waitForCompletionTimeoutInSeconds = waitForCompletionTimeoutInSeconds;
    }
}
