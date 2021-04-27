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
import io.jenkins.plugins.signpath.StepShared.GetSignedArtifactStepInput;
import io.jenkins.plugins.signpath.StepShared.SignPathContext;
import io.jenkins.plugins.signpath.StepShared.SigningRequestStepInputParser;
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

    private String organizationId;
    private String signingRequestId;
    private String outputArtifactPath;

    @DataBoundConstructor
    public GetSignedArtifactStep() {
        super();
    }

    @Override
    public StepExecution start(StepContext context) throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        GetSignedArtifactStepInput input = SigningRequestStepInputParser.ParseInput(this);
        ApiConfiguration apiConfiguration = SigningRequestStepInputParser.ParseApiConfiguration(this);
        SignPathContext signPathContext = SignPathContext.CreateForStep(context, apiConfiguration);
        return new GetSignedArtifactStepExecution(input, signPathContext);
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

    public String getSigningRequestId() {
        return signingRequestId;
    }

    public String getOutputArtifactPath() {
        return outputArtifactPath;
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
    public void setOutputArtifactPath(String outputArtifactPath) {
        this.outputArtifactPath = outputArtifactPath;
    }
}
