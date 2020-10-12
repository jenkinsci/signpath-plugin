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
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.StepShared.SignPathContext;
import io.jenkins.plugins.SignPath.StepShared.SubmitSigningRequestStepInput;
import io.jenkins.plugins.SignPath.StepShared.SigningRequestStepInputParser;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class SubmitSigningRequestStep extends SignPathStepBase {
    private final static String FunctionName = "submitSigningRequest";
    private final static String DisplayName = "Submit SignPath SigningRequest";

    private String organizationId;
    private String projectSlug;
    private String artifactConfigurationSlug;
    private String signingPolicySlug;
    private String inputArtifactPath;
    private String description;
    private Boolean waitForCompletion = false;
    private String outputArtifactPath;

    @DataBoundConstructor
    public SubmitSigningRequestStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        SubmitSigningRequestStepInput input = SigningRequestStepInputParser.ParseInput(this);
        ApiConfiguration apiConfiguration = SigningRequestStepInputParser.ParseApiConfiguration(this);
        SignPathContext signPathContext = SignPathContext.CreateForStep(context, apiConfiguration);

        return new SubmitSigningRequestStepExecution(input, signPathContext);
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

    public Boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
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
    public void setWaitForCompletion(Boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    @DataBoundSetter
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }
}
