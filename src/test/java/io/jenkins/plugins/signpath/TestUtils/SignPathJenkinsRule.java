package io.jenkins.plugins.signpath.TestUtils;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.signpath.SignPathContainer;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nullable;
import java.io.IOException;

public class SignPathJenkinsRule extends JenkinsRule {

    @Nullable
    private final String powerShellExecutable;

    private EnvironmentVariablesNodeProperty environmentVariablesNodeProperty;

    public SignPathJenkinsRule() {
        this(null);
    }

    public SignPathJenkinsRule(@Nullable String powerShellExecutable) {
        this.powerShellExecutable = powerShellExecutable;
    }

    public WorkflowJob createWorkflow(String name, String script) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, name);
        job.setDefinition(new CpsFlowDefinition(String.format("node {%s}", script), true));
        return job;
    }

    @Override
    public void before() throws Throwable {
        super.before();

        if(powerShellExecutable != null) {
            environmentVariablesNodeProperty = new EnvironmentVariablesNodeProperty();
            environmentVariablesNodeProperty.getEnvVars().put(SignPathContainer.POWERSHELL_EXECUTABLE_NAME, powerShellExecutable);
            jenkins.getGlobalNodeProperties().add(environmentVariablesNodeProperty);
        }
    }

    @Override
    public void after() throws Exception {
        if(powerShellExecutable != null) {
            jenkins.getGlobalNodeProperties().remove(environmentVariablesNodeProperty);
        }

        super.after();
    }
}