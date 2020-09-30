package io.jenkins.plugins.SignPath.OriginRetrieval;

import com.google.common.base.CharMatcher;
import hudson.model.Run;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class OriginRetriever {
    private IConfigFileProvider configFileProvider;

    public OriginRetriever(IConfigFileProvider configFileProvider){

        this.configFileProvider = configFileProvider;
    }

    public SigningRequestOriginSubmitModel retrieveForBuild(String rootUrl, Run run) throws IOException {
        BuildData buildData = run.getAction(BuildData.class);
        int buildNumber = run.getNumber();
        String sourceControlManagementType = buildData.scmName;
        String repositoryUrl = buildData.getRemoteUrls().stream().findFirst().get();

        Map.Entry<String, Build> build = buildData.getBuildsByBranchName().entrySet().stream()
                .filter(stringBuildEntry -> stringBuildEntry.getValue().hudsonBuildNumber == buildNumber)
                .findFirst().get();

        String branchName = build.getKey();
        String commitId = build.getValue().getRevision().getSha1String();
        RepositoryMetadataModel repositoryMetadata = new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commitId);

        String jobUrl = run.getUrl();
        String buildUrl = CharMatcher.is('/').trimFrom(rootUrl) + "/" + CharMatcher.is('/').trimFrom(jobUrl);

        File buildConfigFile = configFileProvider.retrieveBuildConfigFile(run);
        TemporaryFile buildSettingsFile = new TemporaryFile();
        try(InputStream in = new FileInputStream(buildConfigFile)) {
            buildSettingsFile.copyFrom(in);
        }

        return new SigningRequestOriginSubmitModel(repositoryMetadata, buildUrl, buildSettingsFile);
    }
}
