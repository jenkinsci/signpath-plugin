package io.jenkins.plugins.signpath.PipelineData;

import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * SIGN-8500. Plain input record for {@link PipelineDataCollector}.
 *
 * <p>Decouples the collector from Jenkins's final {@code WorkflowRun}/{@code WorkflowJob}
 * classes so unit tests can construct inputs directly without mocking. The
 * {@link #from(WorkflowRun)} factory is the only place that touches Jenkins types in
 * the production path.</p>
 */
public final class PipelineDataInputs {
    final long startTimeMillis;
    final String runUrl;
    final FlowDefinition definition;
    final BranchJobProperty branchJobProperty;
    /**
     * The parent of {@link WorkflowJob} when it is a {@link WorkflowMultiBranchProject}.
     * Null when the job is not under a multibranch project. Holding it as Object so
     * callers don't need to import the multibranch type when they don't have one.
     */
    final WorkflowMultiBranchProject multibranchParent;
    final SCMRevisionAction scmRevisionAction;
    final Collection<? extends SCM> scms;
    /**
     * Adapter for {@link GitSCM#getBuildData(Run)} so tests can supply a canned
     * {@link BuildData} per-SCM without mocking GitSCM.
     */
    final Function<GitSCM, BuildData> buildDataLookup;
    /**
     * Path-(d) capture: commit hash of the Jenkinsfile resolved by
     * {@link SignPathDefinitionRevisionCapturer} for standalone {@code CpsScmFlowDefinition}
     * builds. Null if the listener did not fire (pre-existing builds, non-Git SCMs,
     * lightweight checkout failed).
     */
    final SignPathDefinitionRevisionAction definitionRevisionAction;
    /**
     * Path-(a) fallback: the {@link BuildData} actions attached to the run, used to
     * recover the Jenkinsfile commit when {@link #definitionRevisionAction} is absent.
     * The collector matches a candidate {@link BuildData} against the configured
     * {@link CpsScmFlowDefinition#getScm() definition SCM} by remote URL.
     */
    final Collection<BuildData> allBuildData;

    public PipelineDataInputs(long startTimeMillis,
                              String runUrl,
                              FlowDefinition definition,
                              BranchJobProperty branchJobProperty,
                              WorkflowMultiBranchProject multibranchParent,
                              SCMRevisionAction scmRevisionAction,
                              Collection<? extends SCM> scms,
                              Function<GitSCM, BuildData> buildDataLookup,
                              SignPathDefinitionRevisionAction definitionRevisionAction,
                              Collection<BuildData> allBuildData) {
        this.startTimeMillis = startTimeMillis;
        this.runUrl = runUrl;
        this.definition = definition;
        this.branchJobProperty = branchJobProperty;
        this.multibranchParent = multibranchParent;
        this.scmRevisionAction = scmRevisionAction;
        this.scms = scms == null ? Collections.<SCM>emptyList() : scms;
        this.buildDataLookup = buildDataLookup;
        this.definitionRevisionAction = definitionRevisionAction;
        this.allBuildData = allBuildData == null ? Collections.<BuildData>emptyList() : allBuildData;
    }

    /**
     * Production factory: extracts every input the collector needs from a real
     * {@link WorkflowRun}. Performs no I/O — only typed reads.
     */
    public static PipelineDataInputs from(WorkflowRun run) {
        WorkflowJob job = run.getParent();
        WorkflowMultiBranchProject mbp = job.getParent() instanceof WorkflowMultiBranchProject
                ? (WorkflowMultiBranchProject) job.getParent()
                : null;
        return new PipelineDataInputs(
                run.getStartTimeInMillis(),
                run.getUrl(),
                job.getDefinition(),
                job.getProperty(BranchJobProperty.class),
                mbp,
                run.getAction(SCMRevisionAction.class),
                run.getSCMs(),
                git -> git.getBuildData(run),
                run.getAction(SignPathDefinitionRevisionAction.class),
                run.getActions(BuildData.class));
    }

    SCMSource resolveScmSource(String sourceId) {
        return multibranchParent == null ? null : multibranchParent.getSCMSource(sourceId);
    }
}
