package io.jenkins.plugins.SignPath.OriginRetrieval;

import com.google.common.base.CharMatcher;
import hudson.model.Run;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;

import java.util.Map;

public class OriginRetriever {
    public SigningRequestOriginSubmitModel retrieveForBuild(String rootUrl, Run run) {
        String jobUrl = run.getUrl();
        String buildUrl = CharMatcher.is('/').trimFrom(rootUrl) + "/" + CharMatcher.is('/').trimFrom(jobUrl);
        BuildData buildData = run.getAction(BuildData.class);
        int buildNumber = run.getNumber();
        String sourceControlManagementType = buildData.scmName;
        String repositoryUrl = buildData.getRemoteUrls().stream().findFirst().get();

        Map.Entry<String, Build> build = buildData.getBuildsByBranchName().entrySet().stream()
                .filter(stringBuildEntry -> stringBuildEntry.getValue().hudsonBuildNumber == buildNumber)
                .findFirst().get();

        String branchName = build.getKey();
        String commitId = build.getValue().getRevision().getSha1String();

        return new SigningRequestOriginSubmitModel(new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commitId), buildUrl);
    }
}
