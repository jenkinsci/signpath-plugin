package io.jenkins.plugins.sample;

import com.google.common.collect.ImmutableSet;
import com.sun.org.apache.xpath.internal.operations.Bool;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.Set;

public class SignStep extends Step {

    private String organizationId;
    private Boolean waitForCompletion = false;

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

    @DataBoundConstructor
    public SignStep() {}

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SignStepExecution(this, context);
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
            return "signWithSignPath";
        }

        @Override
        public String getDisplayName() {
            return "Signs a given artifact with SignPath";
        }
    }
}
