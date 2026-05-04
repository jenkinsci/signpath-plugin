package io.jenkins.plugins.signpath.PipelineData;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.UserRemoteConfig;
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
import jenkins.branch.Branch;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SIGN-8500 — assembles a {@link PipelineDataDto} from a {@link PipelineDataInputs}
 * record using the conservative rules established by the spike's research.
 *
 * <p>Accepts {@link PipelineDataInputs} (a plain record decoupled from Jenkins's final
 * {@code WorkflowRun}/{@code WorkflowJob} types) so unit tests can drive the rules
 * directly. Production code obtains inputs via {@link PipelineDataInputs#from}.</p>
 *
 * <h2>Build.Definition resolution order</h2>
 * Per the SIGN-8500 follow-up (path d + path a):
 * <ol>
 *   <li>Multibranch — read {@link SCMRevisionAction} attached by {@code SCMBinder}.</li>
 *   <li>Standalone {@code CpsScmFlowDefinition} —
 *     <ol type="a">
 *       <li>Prefer the {@link SignPathDefinitionRevisionAction} populated by
 *           {@link SignPathDefinitionRevisionCapturer} (path d, lightweight-friendly).</li>
 *       <li>Fall back to a {@link BuildData} whose remote URL set intersects the
 *           {@link CpsScmFlowDefinition#getScm() definition SCM}'s remotes
 *           (path a, requires a heavyweight Jenkinsfile fetch or an early
 *           {@code checkout scm}). TOCTOU caveat documented in SIGN-8500.md.</li>
 *     </ol>
 *   </li>
 *   <li>Inline {@code CpsFlowDefinition} — no definition origin exists.</li>
 * </ol>
 */
public final class PipelineDataCollector {

    private static final DateTimeFormatter RFC3339_UTC = DateTimeFormatter.ISO_INSTANT;
    private static final String SCM_TYPE_GIT = "git";

    public PipelineDataExtractionResult collect(PipelineDataInputs inputs) {
        List<String> skipReasons = new ArrayList<>();

        DefinitionDto definition = buildDefinitionOrigin(inputs, skipReasons);
        OriginDto sourceCodeOrigin = sourceCodeOrigin(inputs, skipReasons);

        // SourceCode.Origin is required per the SignPath contract. If we cannot fill
        // it, we cannot emit a valid PipelineDataDto.
        if (sourceCodeOrigin == null) {
            return new PipelineDataExtractionResult(null, skipReasons);
        }

        BuildSystemDto system = buildSystem(skipReasons);
        if (system == null) {
            return new PipelineDataExtractionResult(null, skipReasons);
        }

        BuildDto buildDto = new BuildDto(
                definition,
                new SecurityAssertionsDto(),
                buildStartedAtRfc3339(inputs.startTimeMillis),
                system,
                buildWebUrl(inputs.runUrl));

        SourceCodeDto sourceCodeDto = new SourceCodeDto(sourceCodeOrigin);

        return new PipelineDataExtractionResult(new PipelineDataDto(buildDto, sourceCodeDto), skipReasons);
    }

    static String buildStartedAtRfc3339(long startTimeMillis) {
        return RFC3339_UTC.format(Instant.ofEpochMilli(startTimeMillis).atOffset(ZoneOffset.UTC));
    }

    static BuildSystemDto buildSystem(List<String> skipReasons) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            skipReasons.add("Build.System.Id unavailable: Jenkins instance is not active.");
            return null;
        }
        String id = jenkins.getLegacyInstanceId();
        if (id == null || id.isEmpty()) {
            skipReasons.add("Build.System.Id unavailable: legacyInstanceId is null/empty.");
            return null;
        }
        return new BuildSystemDto(id);
    }

    static String buildWebUrl(String relativeRunUrl) {
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
        return rootUrl + relativeRunUrl;
    }

    /**
     * Returns the build-definition origin for any of the supported job shapes, or null
     * with a skip reason recorded.
     */
    private DefinitionDto buildDefinitionOrigin(PipelineDataInputs inputs, List<String> skipReasons) {
        BranchJobProperty branchProperty = inputs.branchJobProperty;
        if (branchProperty != null) {
            return multibranchDefinitionOrigin(inputs, branchProperty, skipReasons);
        }

        FlowDefinition def = inputs.definition;
        if (def instanceof CpsScmFlowDefinition) {
            return standaloneCpsScmDefinitionOrigin(inputs, (CpsScmFlowDefinition) def, skipReasons);
        }

        // Inline CpsFlowDefinition: there is no repository-backed origin.
        String definitionClass = def == null ? "null" : def.getClass().getName();
        skipReasons.add("Build.Definition skipped: job has no repository-backed pipeline definition (definition="
                + definitionClass + ").");
        return null;
    }

    private DefinitionDto multibranchDefinitionOrigin(PipelineDataInputs inputs,
                                                       BranchJobProperty branchProperty,
                                                       List<String> skipReasons) {
        Branch branch = branchProperty.getBranch();
        SCMHead head = branch.getHead();

        // Locate the SCMSource on the parent multibranch project to recover the
        // configured Git remote URL.
        String repository = null;
        SCMSource source = inputs.resolveScmSource(branch.getSourceId());
        if (source instanceof GitSCMSource) {
            repository = ((GitSCMSource) source).getRemote();
        }

        // Exact revision used to load the Jenkinsfile for this branch build.
        SCMRevisionAction revisionAction = inputs.scmRevisionAction;
        String commitId = null;
        if (revisionAction != null) {
            SCMRevision rev = revisionAction.getRevision();
            if (rev instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                commitId = ((AbstractGitSCMSource.SCMRevisionImpl) rev).getHash();
            }
        }

        // Path to the Jenkinsfile inside the repo. Different multibranch implementations
        // expose getScriptPath() on different BranchProjectFactory subclasses, so we read
        // it reflectively to avoid a hard compile-time dependency on a specific factory.
        String scriptPath = readScriptPathReflectively(inputs.multibranchParent);

        if (repository == null || commitId == null || scriptPath == null) {
            skipReasons.add("Build.Definition skipped: multibranch but one of repository / commitId / scriptPath could not be resolved (repository="
                    + (repository != null) + ", commitId=" + (commitId != null)
                    + ", scriptPath=" + (scriptPath != null) + ").");
            return null;
        }

        return new DefinitionDto(repository, head.getName(), scriptPath, new CommitDto(commitId, null), null);
    }

    /**
     * Standalone {@code CpsScmFlowDefinition} resolution. Tries path (d) first, then
     * path (a). Both paths require a {@link GitSCM}; non-Git SCMs are unsupported in v1.
     */
    private DefinitionDto standaloneCpsScmDefinitionOrigin(PipelineDataInputs inputs,
                                                            CpsScmFlowDefinition def,
                                                            List<String> skipReasons) {
        SCM defScm = def.getScm();
        if (!(defScm instanceof GitSCM)) {
            skipReasons.add("Build.Definition skipped: standalone CpsScmFlowDefinition with non-Git SCM ("
                    + (defScm == null ? "null" : defScm.getClass().getName()) + ").");
            return null;
        }
        GitSCM defGit = (GitSCM) defScm;

        Set<String> defRemotes = collectRemotes(defGit);
        String repository = defRemotes.size() == 1 ? defRemotes.iterator().next() : null;
        if (repository == null) {
            skipReasons.add("Build.Definition skipped: standalone CpsScmFlowDefinition has "
                    + defRemotes.size() + " remote URLs; refusing to guess which is canonical.");
            return null;
        }

        String scriptPath = def.getScriptPath();
        String branchName = singleBranchSpec(defGit);

        // Path (d): SignPathDefinitionRevisionCapturer-attached action.
        String commitId = null;
        SignPathDefinitionRevisionAction action = inputs.definitionRevisionAction;
        if (action != null && action.getCommitId() != null) {
            commitId = action.getCommitId();
        }

        // Path (a) fallback: BuildData whose remote URLs intersect the definition SCM's.
        if (commitId == null) {
            commitId = matchBuildDataCommit(inputs.allBuildData, defRemotes);
        }

        if (commitId == null || scriptPath == null || branchName == null) {
            skipReasons.add("Build.Definition skipped: standalone CpsScmFlowDefinition but one of commitId / scriptPath / branch could not be resolved (commitId="
                    + (commitId != null) + ", scriptPath=" + (scriptPath != null)
                    + ", branch=" + (branchName != null) + ").");
            return null;
        }

        return new DefinitionDto(repository, branchName, scriptPath, new CommitDto(commitId, null), null);
    }

    private static String matchBuildDataCommit(Collection<BuildData> all, Set<String> defRemotes) {
        if (all == null || all.isEmpty()) {
            return null;
        }
        for (BuildData bd : all) {
            if (bd == null) {
                continue;
            }
            Set<String> bdRemotes = bd.getRemoteUrls();
            if (bdRemotes == null || !intersects(bdRemotes, defRemotes)) {
                continue;
            }
            Revision rev = bd.getLastBuiltRevision();
            if (rev == null || rev.getSha1String() == null) {
                continue;
            }
            return rev.getSha1String();
        }
        return null;
    }

    private static Set<String> collectRemotes(GitSCM g) {
        Set<String> out = new HashSet<>();
        for (UserRemoteConfig c : g.getUserRemoteConfigs()) {
            if (c.getUrl() != null) {
                out.add(c.getUrl());
            }
        }
        return out;
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String s : a) {
            if (b.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the single configured branch-spec name for a standalone {@link GitSCM}
     * (e.g. {@code "*&#47;main"} -&gt; {@code "main"}), or null if more than one
     * branch-spec is configured (refuse-to-guess).
     */
    private static String singleBranchSpec(GitSCM git) {
        if (git.getBranches() == null || git.getBranches().size() != 1) {
            return null;
        }
        String name = git.getBranches().get(0).getName();
        if (name == null) {
            return null;
        }
        // Strip a leading "*/" (Git plugin's wildcard remote prefix) so we surface a
        // plain branch name to SignPath.
        if (name.startsWith("*/")) {
            return name.substring(2);
        }
        return name;
    }

    private static String readScriptPathReflectively(WorkflowMultiBranchProject parentProject) {
        try {
            if (parentProject == null) {
                return null;
            }
            Object factory = parentProject.getProjectFactory();
            if (factory == null) {
                return null;
            }
            Object value = factory.getClass().getMethod("getScriptPath").invoke(factory);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Resolves the source-code origin from the run's recorded SCM checkouts.
     *
     * <p>Single-checkout: emits the {@link OriginDto} when the checkout is a
     * {@link GitSCM} with usable {@link BuildData}.</p>
     *
     * <p>Multi-checkout: collapses checkouts that are equivalent in the SignPath sense
     * — same single remote URL <em>and</em> same {@code lastBuiltRevision} SHA1.
     * If every checkout collapses to one logical (remotes, commit) tuple the build
     * effectively has one source and the origin is emitted; otherwise we refuse to
     * guess and surface a diagnostic.</p>
     */
    private OriginDto sourceCodeOrigin(PipelineDataInputs inputs, List<String> skipReasons) {
        Collection<? extends SCM> scms = inputs.scms;
        if (scms.isEmpty()) {
            skipReasons.add("SourceCode.Origin skipped: no SCM checkout was recorded on this build.");
            return null;
        }

        ResolvedCheckout collapsed = null;
        for (SCM scm : scms) {
            ResolvedCheckout resolved = resolveGitCheckout(scm, inputs, scms, skipReasons);
            if (resolved == null) {
                return null;
            }
            if (collapsed == null) {
                collapsed = resolved;
                continue;
            }
            if (!collapsed.isEquivalentTo(resolved)) {
                skipReasons.add("SourceCode.Origin skipped: " + scms.size()
                        + " SCM checkouts recorded and they disagree on remotes or commit; refusing to guess which is canonical. Checkouts: "
                        + describeScms(scms));
                return null;
            }
        }

        return new OriginDto(SCM_TYPE_GIT, collapsed.url, collapsed.branchName,
                new CommitDto(collapsed.sha1, null), null);
    }

    /**
     * Resolves a single SCM into a {@link ResolvedCheckout} or records a skip reason
     * and returns null. The {@code allScms} parameter is used only for diagnostic
     * suffix in the multi-checkout case.
     */
    private static ResolvedCheckout resolveGitCheckout(SCM scm,
                                                       PipelineDataInputs inputs,
                                                       Collection<? extends SCM> allScms,
                                                       List<String> skipReasons) {
        boolean multi = allScms.size() > 1;
        String suffix = multi ? " Checkouts: " + describeScms(allScms) : "";

        if (!(scm instanceof GitSCM)) {
            if (multi) {
                skipReasons.add("SourceCode.Origin skipped: " + allScms.size()
                        + " SCM checkouts recorded and at least one is not a GitSCM ("
                        + scm.getClass().getName() + ")." + suffix);
            } else {
                skipReasons.add("SourceCode.Origin skipped: single checkout is not a GitSCM ("
                        + scm.getClass().getName() + ").");
            }
            return null;
        }

        GitSCM git = (GitSCM) scm;
        BuildData buildData = inputs.buildDataLookup == null ? null : inputs.buildDataLookup.apply(git);
        if (buildData == null) {
            skipReasons.add("SourceCode.Origin skipped: GitSCM had no BuildData for this run "
                    + "(this happens when only a lightweight Jenkinsfile fetch occurred)." + suffix);
            return null;
        }

        Revision rev = buildData.getLastBuiltRevision();
        if (rev == null || rev.getSha1String() == null) {
            skipReasons.add("SourceCode.Origin skipped: BuildData present but lastBuiltRevision is missing." + suffix);
            return null;
        }

        if (buildData.getRemoteUrls() == null || buildData.getRemoteUrls().isEmpty()) {
            skipReasons.add("SourceCode.Origin skipped: BuildData present but no remote URLs." + suffix);
            return null;
        }
        if (buildData.getRemoteUrls().size() > 1) {
            skipReasons.add("SourceCode.Origin skipped: BuildData has " + buildData.getRemoteUrls().size()
                    + " remote URLs; refusing to guess which one is canonical." + suffix);
            return null;
        }

        String url = buildData.getRemoteUrls().iterator().next();
        String branchName = rev.getBranches().isEmpty()
                ? null
                : stripRefsRemotesPrefix(rev.getBranches().iterator().next().getName());
        return new ResolvedCheckout(url, rev.getSha1String(), branchName);
    }

    /**
     * One checkout's identity in the SignPath sense: remote URL + commit SHA1 (which
     * together define "what was built"). The branch name is carried for the emitted
     * DTO but is not part of the equivalence check — two checkouts of the same commit
     * via different branch refs are still the same source.
     */
    private static final class ResolvedCheckout {
        final String url;
        final String sha1;
        final String branchName;

        ResolvedCheckout(String url, String sha1, String branchName) {
            this.url = url;
            this.sha1 = sha1;
            this.branchName = branchName;
        }

        boolean isEquivalentTo(ResolvedCheckout other) {
            return java.util.Objects.equals(url, other.url)
                    && java.util.Objects.equals(sha1, other.sha1);
        }
    }

    /**
     * Diagnostic-only renderer for the multi-SCM skip reason. Lists each SCM's type,
     * its key, and (for {@link GitSCM}) the configured remote URLs and branch specs.
     * The format is intentionally human-readable; the contents are not part of any
     * stable contract.
     */
    private static String describeScms(Collection<? extends SCM> scms) {
        StringBuilder sb = new StringBuilder("[");
        int idx = 0;
        for (SCM scm : scms) {
            if (idx > 0) {
                sb.append(", ");
            }
            sb.append('#').append(idx).append(' ').append(scm.getClass().getName());
            String key = scm.getKey();
            if (key != null && !key.isEmpty()) {
                sb.append(" key=").append(key);
            }
            if (scm instanceof GitSCM) {
                GitSCM g = (GitSCM) scm;
                List<String> remotes = new ArrayList<>();
                if (g.getUserRemoteConfigs() != null) {
                    for (UserRemoteConfig c : g.getUserRemoteConfigs()) {
                        if (c != null && c.getUrl() != null) {
                            remotes.add(c.getUrl());
                        }
                    }
                }
                sb.append(" remotes=").append(remotes);
                List<String> branches = new ArrayList<>();
                if (g.getBranches() != null) {
                    for (hudson.plugins.git.BranchSpec b : g.getBranches()) {
                        if (b != null && b.getName() != null) {
                            branches.add(b.getName());
                        }
                    }
                }
                sb.append(" branches=").append(branches);
            }
            idx++;
        }
        sb.append(']');
        return sb.toString();
    }

    // "refs/remotes/origin/feature/x" -> "feature/x"
    private static String stripRefsRemotesPrefix(String name) {
        if (name == null) {
            return null;
        }
        return name.replaceFirst("^refs/remotes/.*?/", "");
    }
}
