package io.jenkins.plugins.SignPath;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Set;

public class SubmitSigningRequestStep extends Step {

    private String organizationId;
    private Boolean waitForCompletion = false;

    @DataBoundConstructor
    public SubmitSigningRequestStep(){
    }

    public String getOrganizationId(){
        return organizationId;
    }

    public Boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    @DataBoundSetter
    public void setOrganizationId(String organizationId){
        this.organizationId = organizationId;
    }

    @DataBoundSetter
    public void setWaitForCompletion(Boolean waitForCompletion){
        this.waitForCompletion = waitForCompletion;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new SubmitSigningRequestStepExecution(this, context);
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
            return "submitSigningRequest";
        }

        @Override
        public String getDisplayName() {
            return "Submit SignPath SigningRequest";
        }
    }
}
