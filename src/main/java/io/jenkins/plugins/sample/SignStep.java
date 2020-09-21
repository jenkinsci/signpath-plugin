package io.jenkins.plugins.sample;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class SignStep extends Step {


    String token;

    @DataBoundConstructor
    public SignStep() {
        this.token = null;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SignStepExecution(context);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(SignStepExecution.class);
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
