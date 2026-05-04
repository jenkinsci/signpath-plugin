package io.jenkins.plugins.signpath.PipelineData;

import hudson.model.InvisibleAction;

/**
 * SIGN-8500. Records the commit hash from which the Jenkinsfile was fetched for a
 * standalone {@code CpsScmFlowDefinition} build, attached by
 * {@link SignPathDefinitionRevisionCapturer}. Multibranch builds get the equivalent
 * data from the upstream {@code SCMRevisionAction} written by {@code SCMBinder}.
 */
public final class SignPathDefinitionRevisionAction extends InvisibleAction {
    private final String commitId;
    private final String scmKey;

    public SignPathDefinitionRevisionAction(String commitId, String scmKey) {
        this.commitId = commitId;
        this.scmKey = scmKey;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getScmKey() {
        return scmKey;
    }
}
