package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;

import java.util.Map;

public class OriginRetriever {
    public OriginRetriever(){
    }

    public SigningRequestOriginSubmitModel retrieveForBuild(Run run) {
        BuildData buildData = run.getAction(BuildData.class);
        int buildNumber = run.getNumber();
        String sourceControlManagementType = buildData.scmName;
        String repositoryUrl = buildData.getRemoteUrls().stream().findFirst().get();

        Map.Entry<String, Build> build = buildData.getBuildsByBranchName().entrySet().stream()
                .filter(stringBuildEntry -> stringBuildEntry.getValue().hudsonBuildNumber == buildNumber)
                .findFirst().get();

        String branchName = build.getKey();
        String commitId = build.getValue().getRevision().getSha1String();
        String buildUrl = run.getUrl();

        return new SigningRequestOriginSubmitModel(new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commitId), buildUrl);
    }
}
