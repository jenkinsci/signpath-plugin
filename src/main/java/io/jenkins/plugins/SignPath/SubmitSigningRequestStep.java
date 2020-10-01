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
import io.jenkins.plugins.SignPath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.SignPath.OriginRetrieval.IOriginRetriever;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
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

public class SubmitSigningRequestStep extends Step {
    private final static String FunctionName = "submitSigningRequest";
    private final static String DisplayName = "Submit SignPath SigningRequest";

    private String apiUrl =  "https://app.signpath.io/api/";
    private String ciUserToken;
    private String organizationId;
    private String projectSlug;
    private String artifactConfigurationSlug;
    private String signingPolicySlug;
    private String inputArtifactPath;
    private String description;
    private int serviceUnavailableTimeoutInSeconds = 600;
    private int uploadAndDownloadRequestTimeoutInSeconds = 300;
    private Boolean waitForCompletion = false;
    private String outputArtifactPath;
    private int waitForCompletionTimeoutInSeconds = 600;

    @DataBoundConstructor
    public SubmitSigningRequestStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException {
        TaskListener listener = context.get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = context.get(Run.class);
        Launcher launcher = context.get(Launcher.class);
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootUrl = jenkins.getConfiguredRootUrl();

        // TODO SIGN-3326: Add to dependency-injection
        ISecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        IOriginRetriever originRetriever = new OriginRetriever(new DefaultConfigFileProvider(run), run, jenkinsRootUrl);
        IArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);
        IPowerShellExecutor pwsh = new PowerShellExecutor("pwsh");
        ApiConfiguration apiConfiguration = new ApiConfiguration(new URL(getApiUrl()),
                getServiceUnavailableTimeoutInSeconds(),
                getUploadAndDownloadRequestTimeoutInSeconds(),
                getWaitForCompletionTimeoutInSeconds());
        ISignPathFacadeFactory signPathFacadeFactory = new SignPathPowerShellFacadeFactory(pwsh, apiConfiguration);

        return new SubmitSigningRequestStepExecution(this, context, logger, secretRetriever, originRetriever,artifactFileManager, signPathFacadeFactory);
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

    public String getProjectSlug() {
        return projectSlug;
    }

    public String getArtifactConfigurationSlug() {
        return artifactConfigurationSlug;
    }

    public String getSigningPolicySlug() {
        return signingPolicySlug;
    }

    public String getInputArtifactPath() {
        return inputArtifactPath;
    }

    public String getDescription() {
        return description;
    }

    public int getServiceUnavailableTimeoutInSeconds() {
        return serviceUnavailableTimeoutInSeconds;
    }

    public int getUploadAndDownloadRequestTimeoutInSeconds() {
        return uploadAndDownloadRequestTimeoutInSeconds;
    }

    public Boolean getWaitForCompletion() {
        return waitForCompletion;
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
    public void setProjectSlug(String projectSlug) {
        this.projectSlug = projectSlug;
    }

    @DataBoundSetter
    public void setArtifactConfigurationSlug(String artifactConfigurationSlug) {
        this.artifactConfigurationSlug = artifactConfigurationSlug;
    }

    @DataBoundSetter
    public void setSigningPolicySlug(String signingPolicySlug) {
        this.signingPolicySlug = signingPolicySlug;
    }

    @DataBoundSetter
    public void setInputArtifactPath(String inputArtifactPath) {
        this.inputArtifactPath = inputArtifactPath;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
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
    public void setWaitForCompletion(Boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
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
