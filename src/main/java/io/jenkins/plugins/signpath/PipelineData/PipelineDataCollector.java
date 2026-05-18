package io.jenkins.plugins.signpath.PipelineData;

import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import io.jenkins.plugins.signpath.PipelineData.Model.BuildDto;
import io.jenkins.plugins.signpath.PipelineData.Model.BuildSystemDto;
import io.jenkins.plugins.signpath.PipelineData.Model.CommitDto;
import io.jenkins.plugins.signpath.PipelineData.Model.DefinitionDto;
import io.jenkins.plugins.signpath.PipelineData.Model.OriginDto;
import io.jenkins.plugins.signpath.PipelineData.Model.PipelineDataDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SecurityAssertionsDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SourceCodeDto;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import jenkins.plugins.git.AbstractGitSCMSource;

/**
 * Builds the SignPath {@code PipelineDataDto} for a Jenkins {@code WorkflowRun}.
 *
 * <p>SIGN-8581 only fills {@code Build.Definition} for Multibranch pipelines;
 * standalone {@code CpsScmFlowDefinition} and inline {@code CpsFlowDefinition}
 * leave the field {@code null} with an info log. {@code SourceCode.Origin} is
 * always required by the contract — if it cannot be reliably resolved from
 * {@code BuildData}, the entire {@code PipelineData} is dropped and the
 * signing request is submitted without it.</p>
 */
public final class PipelineDataCollector {
    private static final DateTimeFormatter RFC3339_UTC = DateTimeFormatter.ISO_INSTANT;
    private static final String SCM_TYPE_GIT = "git";
    private static final String BUILD_SYSTEM_ID = "jenkins";
    /**
     * Prefix used to pick the branch-tracking ref out of
     * {@code BuildData.lastBuiltRevision.getBranches()}. The Git plugin stores
     * the branch under {@code refs/remotes/<remote>/<name>} after a fetch
     * (e.g. {@code refs/remotes/origin/main}); tag refs appear separately as
     * {@code refs/tags/<name>}. We keep the prefix intact in the emitted DTO
     * per the SIGN-8581 ticket — only the filter uses it.
     */
    private static final String BRANCH_REF_PREFIX = "refs/remotes/";

    private final PrintStream logger;

    public PipelineDataCollector(PrintStream logger) {
        this.logger = logger;
    }

    /**
     * Returns a populated {@code PipelineDataDto}, or {@code null} when it
     * cannot be filled in reliably (see ticket rules). Diagnostic messages
     * are written to {@code logger}; callers should still submit the signing
     * request when this method returns {@code null}.
     */
    public PipelineDataDto collect(WorkflowRun run) {
        WorkflowJob job = run.getParent();
        FlowDefinition definition = job.getDefinition();

        OriginDto sourceCodeOrigin = sourceCodeOrigin(run, definition);
        if (sourceCodeOrigin == null) {
            return null;
        }

        DefinitionDto buildDefinition = buildDefinition(run, definition);

        BuildDto build = new BuildDto(
                buildDefinition,
                new SecurityAssertionsDto(),
                rfc3339Utc(run.getStartTimeInMillis()),
                new BuildSystemDto(BUILD_SYSTEM_ID),
                buildWebUrl(run));

        return new PipelineDataDto(build, new SourceCodeDto(sourceCodeOrigin));
    }

    // ---- Build.Definition ----------------------------------------------------

    private DefinitionDto buildDefinition(WorkflowRun run, FlowDefinition definition) {
        BranchJobProperty branchProperty = run.getParent().getProperty(BranchJobProperty.class);
        if (branchProperty != null) {
            return multibranchBuildDefinition(run, branchProperty);
        }
        if (definition instanceof CpsScmFlowDefinition || definition instanceof CpsFlowDefinition) {
            logger.println("[SignPath] Build definition can only be reliably determined for Multibranch pipelines.");
            return null;
        }
        // Custom / third-party FlowDefinition — same outcome as inline.
        String defClassName = (definition == null) ? "null" : definition.getClass().getName();
        logger.printf("[SignPath] Build.Definition omitted: unsupported FlowDefinition class %s. "
                + "Build definition is only emitted for Multibranch pipelines.%n", defClassName);
        return null;
    }

    private DefinitionDto multibranchBuildDefinition(WorkflowRun run, BranchJobProperty branchProperty) {
        WorkflowJob job = run.getParent();
        if (!(job.getParent() instanceof WorkflowMultiBranchProject)) {
            logger.println("[SignPath] Build.Definition omitted: branch job parent is not a WorkflowMultiBranchProject.");
            return null;
        }
        WorkflowMultiBranchProject parent = (WorkflowMultiBranchProject) job.getParent();

        SCMSource scmSource = parent.getSCMSource(branchProperty.getBranch().getSourceId());
        if (!(scmSource instanceof GitSCMSource)) {
            String type = (scmSource == null) ? "null" : scmSource.getClass().getName();
            logger.printf("[SignPath] Build.Definition omitted: Multibranch SCMSource is %s, only GitSCMSource is supported.%n", type);
            return null;
        }
        String repository = ((GitSCMSource) scmSource).getRemote();

        if (!(parent.getProjectFactory() instanceof WorkflowBranchProjectFactory)) {
            String type = parent.getProjectFactory() == null ? "null" : parent.getProjectFactory().getClass().getName();
            logger.printf("[SignPath] Build.Definition omitted: BranchProjectFactory is %s, only WorkflowBranchProjectFactory is supported.%n", type);
            return null;
        }
        String scriptPath = ((WorkflowBranchProjectFactory) parent.getProjectFactory()).getScriptPath();

        SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
        SCMRevision revision = revAction == null ? null : revAction.getRevision();
        if (revision == null) {
            logger.println("[SignPath] Build.Definition omitted: SCMRevisionAction missing on Multibranch run.");
            return null;
        }
        String commitId = extractGitCommitHash(revision);
        if (commitId == null) {
            logger.println("[SignPath] Build.Definition omitted: SCMRevisionAction does not carry a Git revision.");
            return null;
        }
        SCMHead head = revision.getHead();
        String branchName = (head == null) ? null : head.getName();
        if (branchName == null) {
            logger.println("[SignPath] Build.Definition omitted: SCMRevisionAction.revision.head is null.");
            return null;
        }

        return new DefinitionDto(repository, branchName, scriptPath, new CommitDto(commitId, null), null);
    }

    private static String extractGitCommitHash(SCMRevision rev) {
        if (rev instanceof AbstractGitSCMSource.SCMRevisionImpl) {
            return ((AbstractGitSCMSource.SCMRevisionImpl) rev).getHash();
        }
        return null;
    }

    // ---- SourceCode.Origin ---------------------------------------------------

    private OriginDto sourceCodeOrigin(WorkflowRun run, FlowDefinition definition) {
        BuildData buildData = run.getAction(BuildData.class);
        if (buildData == null) {
            logger.println("[SignPath] Pipeline Information for SignPath is currently only supported with git repositories.");
            return null;
        }

        SCM checkoutScm = findFirstSupportedSourceScm(run);
        if (checkoutScm == null) {
            logger.println("[SignPath] Pipeline Information for SignPath is currently only supported with git repositories.");
            return null;
        }

        Set<String> remoteUrls = buildData.getRemoteUrls();
        if (remoteUrls == null || remoteUrls.size() != 1) {
            int count = remoteUrls == null ? 0 : remoteUrls.size();
            logger.printf("[SignPath] PipelineData dropped: BuildData has %d remote URLs (expected exactly 1).%n", count);
            return null;
        }
        String url = remoteUrls.iterator().next();

        Revision lastBuilt = buildData.getLastBuiltRevision();
        if (lastBuilt == null || lastBuilt.getSha1String() == null) {
            logger.println("[SignPath] PipelineData dropped: BuildData has no lastBuiltRevision SHA.");
            return null;
        }
        String commitId = lastBuilt.getSha1String();

        String branch = resolveSourceCodeBranch(run, definition, checkoutScm, lastBuilt);
        if (branch == null) {
            return null;
        }

        return new OriginDto(SCM_TYPE_GIT, url, branch, new CommitDto(commitId, null), null);
    }

    /**
     * Returns the first {@link GitSCM} attached to the run, or {@code null} when
     * no Git checkout was recorded. Multi-checkout pipelines surface as
     * multiple SCMs; per ticket we still proceed using the single
     * {@code BuildData} attached to the run for the Origin fields.
     */
    private static SCM findFirstSupportedSourceScm(WorkflowRun run) {
        for (SCM scm : run.getSCMs()) {
            if (scm instanceof GitSCM) {
                return scm;
            }
        }
        return null;
    }

    private String resolveSourceCodeBranch(WorkflowRun run,
                                           FlowDefinition definition,
                                           SCM checkoutScm,
                                           Revision lastBuilt) {
        // Multibranch: take SCMRevisionAction.revision.head.name verbatim
        // (no prefix to strip). PR builds legitimately yield "PR-XY".
        if (run.getParent().getProperty(BranchJobProperty.class) != null) {
            SCMRevisionAction revAction = run.getAction(SCMRevisionAction.class);
            if (revAction == null || revAction.getRevision() == null) {
                logger.println("[SignPath] PipelineData dropped: Multibranch build has no SCMRevisionAction; cannot determine SourceCode.Origin.Branch.");
                return null;
            }
            return revAction.getRevision().getHead().getName();
        }

        // CpsScmFlowDefinition or CpsFlowDefinition: read BuildData.lastBuiltRevision.getBranches()
        // and keep the refs/heads (or refs/tags) prefix.
        List<BranchSpec> branchSpecs = (checkoutScm instanceof GitSCM)
                ? ((GitSCM) checkoutScm).getBranches()
                : null;
        if (branchSpecs != null && branchSpecs.size() > 1) {
            logger.printf("[SignPath] PipelineData dropped: shape=%s has %d BranchSpec entries; "
                            + "branch cannot be reliably determined. BranchSpecs=%s, BuildData branches=%s.%n",
                    shapeName(definition), branchSpecs.size(),
                    formatBranchSpecs(branchSpecs), formatBranches(lastBuilt.getBranches()));
            return null;
        }

        if (lastBuilt.getBranches() == null || lastBuilt.getBranches().isEmpty()) {
            logger.printf("[SignPath] PipelineData dropped: shape=%s has no branch entries in BuildData.lastBuiltRevision. "
                            + "BranchSpecs=%s.%n",
                    shapeName(definition), formatBranchSpecs(branchSpecs));
            return null;
        }

        String chosen = pickFirstBranchRef(lastBuilt.getBranches());
        if (chosen == null) {
            logger.printf("[SignPath] PipelineData dropped: shape=%s — no branch entry starts with %s. "
                            + "Branches=%s, BranchSpecs=%s.%n",
                    shapeName(definition), BRANCH_REF_PREFIX,
                    formatBranches(lastBuilt.getBranches()), formatBranchSpecs(branchSpecs));
            return null;
        }
        return chosen;
    }

    /**
     * Returns the first {@link Branch} whose ref name starts with
     * {@link #BRANCH_REF_PREFIX} (i.e. it is a remote-tracking branch ref),
     * preserving the full prefix in the returned value. Tag-only refs
     * (e.g. {@code refs/tags/v1.0}) are skipped because they don't identify a
     * branch on their own.
     */
    static String pickFirstBranchRef(java.util.Collection<Branch> branches) {
        for (Branch b : branches) {
            if (b != null && b.getName() != null && b.getName().startsWith(BRANCH_REF_PREFIX)) {
                return b.getName();
            }
        }
        return null;
    }

    private static String shapeName(FlowDefinition definition) {
        if (definition instanceof CpsScmFlowDefinition) {
            return "CpsScmFlowDefinition";
        }
        if (definition instanceof CpsFlowDefinition) {
            return "CpsFlowDefinition";
        }
        return definition == null ? "null" : definition.getClass().getName();
    }

    private static String formatBranchSpecs(List<BranchSpec> specs) {
        if (specs == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (BranchSpec s : specs) {
            if (!first) sb.append(", ");
            sb.append(s == null ? "null" : s.getName());
            first = false;
        }
        return sb.append(']').toString();
    }

    private static String formatBranches(java.util.Collection<Branch> branches) {
        if (branches == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Branch b : branches) {
            if (!first) sb.append(", ");
            sb.append(b == null ? "null" : b.getName());
            first = false;
        }
        return sb.append(']').toString();
    }

    // ---- Build.WebUrl / StartedAt -------------------------------------------

    static String rfc3339Utc(long epochMillis) {
        return RFC3339_UTC.format(Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC));
    }

    static String buildWebUrl(Run<?, ?> run) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }
        String rootUrl = jenkins.getRootUrl();
        if (rootUrl == null) {
            return null;
        }
        if (!rootUrl.endsWith("/")) {
            rootUrl = rootUrl + "/";
        }
        return rootUrl + run.getUrl();
    }
}
