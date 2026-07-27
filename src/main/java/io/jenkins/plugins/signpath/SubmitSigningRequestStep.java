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
import lombok.Getter;
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

    @Getter
    private String organizationId;
    @Getter
    private String projectSlug;
    @Getter
    private String artifactConfigurationSlug;
    @Getter
    private String signingPolicySlug;
    @Getter
    private String inputArtifactPath;
    @Getter
    private String description;
    @Getter
    private String outputArtifactPath;
    private boolean waitForCompletion = false;
    @Getter
    private Map<String, String> parameters;
    @Getter
    private String inputArtifactRetrievalUrl;
    @Getter
    private Map<String, String> inputArtifactRetrievalHttpHeaders;

    @DataBoundConstructor
    public SubmitSigningRequestStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        if (getOutputArtifactPath() != null && !getOutputArtifactPath().isEmpty() && !getWaitForCompletion()) {
            throw new SignPathStepInvalidArgumentException("outputArtifactPath can only be set if waitForCompletion is true");
        }

        if (getInputArtifactRetrievalHttpHeaders() != null && !getInputArtifactRetrievalHttpHeaders().isEmpty()
                && (getInputArtifactRetrievalUrl() == null || getInputArtifactRetrievalUrl().isEmpty())) {
            throw new SignPathStepInvalidArgumentException("inputArtifactRetrievalHttpHeaders can only be provided together with inputArtifactRetrievalUrl");
        }

        SubmitSigningRequestStepInput input = new SubmitSigningRequestStepInput(
                ensureValidUUID(getOrganizationIdWithGlobal(), "organizationId"),
                ensureNotNull(getApiTokenCredentialId(), "apiTokenCredentialId"),
                ensureNotNull(getProjectSlug(), "projectSlug"),
                getArtifactConfigurationSlug(),
                ensureNotNull(getSigningPolicySlug(), "signingPolicySlug"),
                ensureNotNull(getInputArtifactPath(), "inputArtifactPath"),
                getDescription(),
                getOutputArtifactPath(),
                getParameters(),
                getWaitForCompletion(),
                getInputArtifactRetrievalUrl(),
                getInputArtifactRetrievalHttpHeaders());

        ApiConfiguration apiConfiguration = getAndValidateApiConfiguration();
        SignPathContainer container = SignPathContainer.build(context, apiConfiguration);

        return new SubmitSigningRequestStepExecution(input,
                container.getSecretRetriever(),
                container.getArtifactFileManager(),
                container.getPipelineConnectorFacadeFactory(),
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

    public String getOrganizationIdWithGlobal() throws SignPathStepInvalidArgumentException {
        return getWithGlobalConfig(
            organizationId,
            SignPathPluginGlobalConfiguration::getOrganizationId,
            "organizationId", true);
    }

    public boolean getWaitForCompletion() {
        return waitForCompletion;
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
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }

    @DataBoundSetter
    public void setWaitForCompletion(boolean waitForCompletion) {
        this.waitForCompletion = waitForCompletion;
    }

    @DataBoundSetter
    public void setParameters (Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @DataBoundSetter
    public void setInputArtifactRetrievalUrl(String inputArtifactRetrievalUrl) {
        this.inputArtifactRetrievalUrl = inputArtifactRetrievalUrl;
    }

    @DataBoundSetter
    public void setInputArtifactRetrievalHttpHeaders(Map<String, String> inputArtifactRetrievalHttpHeaders) {
        this.inputArtifactRetrievalHttpHeaders = inputArtifactRetrievalHttpHeaders;
    }

}
