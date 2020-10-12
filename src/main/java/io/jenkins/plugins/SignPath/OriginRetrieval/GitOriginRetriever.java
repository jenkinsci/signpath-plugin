package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of the
 *
 * @see OriginRetriever interface
 * that can only handle
 * @see BuildData (from the corresponding git plugin)
 */
public class GitOriginRetriever implements OriginRetriever {
    // we hard-code git, because we retrieve the data from hudson.plugins.git.util.BuildData (git only)
    private static final String sourceControlManagementType = "git";

    private final ConfigFileProvider configFileProvider;
    private final Run<?, ?> run;
    private final String rootUrl;
    private PrintStream logger;

    public GitOriginRetriever(ConfigFileProvider configFileProvider, Run<?, ?> run, String rootUrl) {
        this.configFileProvider = configFileProvider;
        this.run = run;
        this.rootUrl = rootUrl;
    }

    @Override
    public SigningRequestOriginModel retrieveOrigin() throws IOException, OriginNotRetrievableException {
        BuildData buildData = run.getAction(BuildData.class);
        int buildNumber = run.getNumber();
        Map.Entry<String, Build> matchingBuild = findMatchingBuild(buildData, buildNumber);
        String repositoryUrl = getSingleRemoteUrl(buildData, buildNumber);
        String branchName = parseBranchName(matchingBuild.getKey());
        String commitId = matchingBuild.getValue().getRevision().getSha1String();
        RepositoryMetadataModel repositoryMetadata = new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commitId);

        String jobUrl = run.getUrl();
        String buildUrl = StringUtils.strip(rootUrl, "/") + "/" + StringUtils.strip(jobUrl, "/");
        TemporaryFile buildSettingsFile = getBuildSettingsFile();
        return new SigningRequestOriginModel(repositoryMetadata, buildUrl, buildSettingsFile);
    }

    private String parseBranchName(String key) {
        return key.replaceFirst("^refs/remotes/.*?/", "");
    }

    private TemporaryFile getBuildSettingsFile() throws IOException {
        File buildConfigFile = configFileProvider.retrieveBuildConfigFile();
        TemporaryFile buildSettingsFile = new TemporaryFile();
        try (InputStream in = new FileInputStream(buildConfigFile)) {
            buildSettingsFile.copyFrom(in);
        }
        return buildSettingsFile;
    }

    private String getSingleRemoteUrl(BuildData buildData, int buildNumber) throws OriginNotRetrievableException {
        Set<String> remoteUrls = buildData.getRemoteUrls();
        if (remoteUrls.size() == 0) {
            throw new OriginNotRetrievableException(String.format("No remote urls for build with build number '%d' found.", buildNumber));
        }

        if (remoteUrls.size() > 1) {
            throw new OriginNotRetrievableException(String.format("%d remote urls for build with build number '%d' found. This is not supported.", remoteUrls.size(), buildNumber));
        }

        return remoteUrls.stream().findFirst().get();
    }

    private Map.Entry<String, Build> findMatchingBuild(BuildData buildData, int buildNumber) throws OriginNotRetrievableException {
        List<Map.Entry<String, Build>> matchingBuilds = buildData.getBuildsByBranchName().entrySet().stream()
                .filter(buildByBranchName -> buildByBranchName.getValue().hudsonBuildNumber == buildNumber)
                .collect(Collectors.toList());

        if (matchingBuilds.size() == 0) {
            throw new OriginNotRetrievableException(String.format("No builds with build number '%d' found.", buildNumber));
        }

        if (matchingBuilds.size() > 1) {
            throw new OriginNotRetrievableException(String.format("%d builds with build number '%d' found. This is not supported.", matchingBuilds.size(), buildNumber));
        }

        return matchingBuilds.get(0);
    }
}
