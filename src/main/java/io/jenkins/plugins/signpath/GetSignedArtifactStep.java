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
import java.util.Set;

/**
 * Represents the getSignedArtifact step that is executable via pipeline-script
 * For a how-to use example see *EndToEnd tests
 */
public class GetSignedArtifactStep extends SignPathStepBase {
    private final static String FunctionName = "getSignedArtifact";
    private final static String DisplayName = "Download SignPath Signed Artifact";

    @Deprecated
    private String organizationId;
    private String signingRequestId;
    private String outputArtifactPath;

    @DataBoundConstructor
    public GetSignedArtifactStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        GetSignedArtifactStepInput input =  new GetSignedArtifactStepInput(
                ensureValidUUID(getOrganizationIdWithGlobal(), "organizationId"),
                ensureValidUUID(getSigningRequestId(), "signingRequestId"),
                ensureNotNull(getTrustedBuildSystemTokenCredentialId(), "trustedBuildSystemTokenCredentialId"),
                ensureNotNull(getApiTokenCredentialId(), "apiTokenCredentialId"),
                ensureNotNull(getOutputArtifactPath(), "outputArtifactPath"));

        ApiConfiguration apiConfiguration = getAndValidateApiConfiguration();
        SignPathContainer container = SignPathContainer.build(context, apiConfiguration);
        return new GetSignedArtifactStepExecution(input,
                container.getSecretRetriever(),
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

    @Deprecated
    public String getOrganizationId() throws SignPathStepInvalidArgumentException {
        return organizationId;
    }

    public String getOrganizationIdWithGlobal() throws SignPathStepInvalidArgumentException {
        return getWithGlobalConfig(
            organizationId,
            SignPathPluginGlobalConfiguration::getDefaultOrganizationId,
            "organizationId", true);
    }

    public String getSigningRequestId() {
        return signingRequestId;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
    }

    @Deprecated
    @DataBoundSetter
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @DataBoundSetter
    public void setSigningRequestId(String signingRequestId) {
        this.signingRequestId = signingRequestId;
    }

    @DataBoundSetter
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }
}
