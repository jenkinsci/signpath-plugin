package io.jenkins.plugins.signpath;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Represents the submitSigningRequestStep step that is executable via pipeline-script
 * Encapsulates both the sync and async version of the step
 * -&gt; thus the waitForCompletion param is used to toggle between the two modes
 * For a how-to use example see *EndToEnd tests
 */
public class SubmitSigningRequestStep extends SignPathStepBase {
    private final static String FunctionName = "submitSigningRequest";
    private final static String DisplayName = "Submit SignPath Signing Request";

    private String organizationId;
    private String projectSlug;
    private String artifactConfigurationSlug;
    private String signingPolicySlug;
    private String inputArtifactPath;
    private String description;
    private boolean waitForCompletion = false;
    private String outputArtifactPath;
    private Map<String, String> parameters;

    @DataBoundConstructor
    public SubmitSigningRequestStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        boolean waitForCompletion = getWaitForCompletion();
        String outputArtifactPath = waitForCompletion ? ensureNotNull(getOutputArtifactPath(), "outputArtifactPath") : null;
        SubmitSigningRequestStepInput input = new SubmitSigningRequestStepInput(
                ensureValidUUID(getOrganizationIdWithGlobal(), "organizationId"),
                ensureNotNull(getTrustedBuildSystemTokenCredentialIdWithGlobal(), "trustedBuildSystemTokenCredentialId"),
                ensureNotNull(getApiTokenCredentialId(), "apiTokenCredentialId"),
                ensureNotNull(getProjectSlug(), "projectSlug"),
                getArtifactConfigurationSlug(),
                ensureNotNull(getSigningPolicySlug(), "signingPolicySlug"),
                ensureNotNull(getInputArtifactPath(), "inputArtifactPath"),
                getDescription(),
                outputArtifactPath,
                getParameters(),
                waitForCompletion);

        ApiConfiguration apiConfiguration = getAndValidateApiConfiguration();
        SignPathContainer container = SignPathContainer.build(context, apiConfiguration);

        CheckDeprecatedParametersUsage(container);

        return new SubmitSigningRequestStepExecution(input,
                container.getSecretRetriever(),
                container.getOriginRetriever(),
                container.getArtifactFileManager(),
                container.getSignPathFacadeFactory(),
                container.getTaskListener(),
                container.getStepContext());
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

    public String getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationIdWithGlobal() throws SignPathStepInvalidArgumentException {
        return getWithGlobalConfig(
            organizationId,
            SignPathPluginGlobalConfiguration::getOrganizationId,
            "organizationId");
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

    public boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }
    
    public Map<String, String> getParameters () {
        return parameters;
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
    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    @DataBoundSetter
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }
    
    @DataBoundSetter
    public void setParameters (Map<String, String> parameters) {
        this.parameters = parameters;
    }

    private void CheckDeprecatedParametersUsage(SignPathContainer container) {
        if (this.getApiUrl() != null && !this.getApiUrl().isEmpty()) {
            logStepParameterDeprecationWarning(container.getTaskListener(), "apiUrl", "Api URL");
        }
        
        if(this.getTrustedBuildSystemTokenCredentialId() != null && !this.getTrustedBuildSystemTokenCredentialId().isEmpty()) {
            logStepParameterDeprecationWarning(container.getTaskListener(), "trustedBuildSystemTokenCredentialId", "Trusted Build System Credential ID");
        }

        if(this.getOrganizationId() != null && !this.getOrganizationId().isEmpty()) {
            logStepParameterDeprecationWarning(container.getTaskListener(), "organizationId", "Organization ID");
        }
    }
}
