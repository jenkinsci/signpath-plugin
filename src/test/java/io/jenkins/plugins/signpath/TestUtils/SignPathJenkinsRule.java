package io.jenkins.plugins.signpath.TestUtils;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class SignPathJenkinsRule extends JenkinsRule {

    public SignPathJenkinsRule() {
    }

    public WorkflowJob createWorkflow(String name, String script) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, name);
        job.setDefinition(new CpsFlowDefinition(String.format("node {%s}", script), true));
        return job;
    }
}