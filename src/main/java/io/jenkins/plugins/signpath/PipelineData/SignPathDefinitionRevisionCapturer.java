package io.jenkins.plugins.signpath.PipelineData;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.scm.SCM;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMRevision;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SIGN-8500. Captures the Jenkinsfile commit for standalone {@code CpsScmFlowDefinition}
 * pipelines and attaches it to the {@link WorkflowRun} as a
 * {@link SignPathDefinitionRevisionAction}.
 *
 * <p>Implements path (d) of the SIGN-8500 follow-up research: the upstream
 * {@code CpsScmFlowDefinition.create(...)} resolves the script via
 * {@code SCMFileSystem.of(job, scm)} but never writes the resolved revision back to
 * the run. We re-open {@code SCMFileSystem.of(job, scm)} from
 * {@link #onCreated(FlowExecution)} (which fires after the script has been fetched
 * but before any user step runs) and persist the resolved git commit ourselves.</p>
 *
 * <p>For Multibranch jobs the commit is already available via
 * {@code SCMRevisionAction}, so this listener no-ops there. For inline
 * {@code CpsFlowDefinition} jobs there is no SCM origin to capture.</p>
 *
 * <p><b>TOCTOU note:</b> there is a small window (typically sub-second) between the
 * upstream fetch and our second {@code SCMFileSystem.of(...)} call during which an
 * additional push to the same branch could change the resolved tip. The risk is
 * documented in the SIGN-8500 follow-up and is materially smaller than the v1.4
 * "checkout scm" fallback's TOCTOU window of one or more pipeline steps.</p>
 */
@Extension
public final class SignPathDefinitionRevisionCapturer extends FlowExecutionListener {

    private static final Logger LOGGER =
            Logger.getLogger(SignPathDefinitionRevisionCapturer.class.getName());

    @Override
    public void onCreated(@NonNull FlowExecution exec) {
        try {
            Run<?, ?> run = runOf(exec);
            if (!(run instanceof WorkflowRun)) {
                return;
            }
            WorkflowRun workflowRun = (WorkflowRun) run;

            if (workflowRun.getAction(SignPathDefinitionRevisionAction.class) != null) {
                return;
            }

            WorkflowJob job = workflowRun.getParent();
            FlowDefinition def = job.getDefinition();
            if (!(def instanceof CpsScmFlowDefinition)) {
                return;
            }
            SCM scm = ((CpsScmFlowDefinition) def).getScm();

            String commit = resolveCommit(job, scm);
            if (commit == null) {
                return;
            }

            workflowRun.addAction(new SignPathDefinitionRevisionAction(commit, scm.getKey()));
            workflowRun.save();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "SignPath: failed to capture definition revision for standalone pipeline", ex);
        }
    }

    @CheckForNull
    private static String resolveCommit(WorkflowJob job, SCM scm) throws Exception {
        try (SCMFileSystem fs = SCMFileSystem.of(job, scm)) {
            if (fs == null) {
                return null;
            }
            SCMRevision rev = fs.getRevision();
            if (rev != null) {
                return rev.toString();
            }
            if (fs instanceof GitSCMFileSystem) {
                return readGitCommitId((GitSCMFileSystem) fs);
            }
            return null;
        }
    }

    /**
     * {@code GitSCMFileSystem} caches the resolved commit in a private {@code commitId}
     * field (populated by the constructor from {@code repo.findRef(head).getObjectId()}
     * even when the {@link SCMRevision} is null, which is the case for the
     * {@code SCMFileSystem.of(Item, SCM)} overload that {@code CpsScmFlowDefinition}
     * uses). The package-private {@code getCommitId()} accessor exists upstream but
     * isn't reachable from this package; reflection on the field is the workaround
     * documented in the SIGN-8500 follow-up. Long-term fix is to upstream a public
     * getter on {@code git-plugin}.
     */
    @CheckForNull
    private static String readGitCommitId(GitSCMFileSystem gfs) {
        try {
            Field f = GitSCMFileSystem.class.getDeclaredField("commitId");
            f.setAccessible(true);
            Object value = f.get(gfs);
            if (value instanceof org.eclipse.jgit.lib.ObjectId) {
                return ((org.eclipse.jgit.lib.ObjectId) value).getName();
            }
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.FINE, "SignPath: GitSCMFileSystem.commitId not accessible via reflection", ex);
            return null;
        }
    }

    @CheckForNull
    private static Run<?, ?> runOf(FlowExecution exec) throws Exception {
        FlowExecutionOwner owner = exec.getOwner();
        if (owner == null) {
            return null;
        }
        Object executable = owner.getExecutable();
        return (executable instanceof Run) ? (Run<?, ?>) executable : null;
    }
}
