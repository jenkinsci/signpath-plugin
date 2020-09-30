package io.jenkins.plugins.SignPath.OriginRetrieval;

public class RepositoryMetadataModel {
    private final String sourceControlManagementType;
    private final String repositoryUrl;
    private final String branchName;
    private final String commitId;

    public RepositoryMetadataModel(String sourceControlManagementType, String repositoryUrl, String branchName, String commitId) {
        this.sourceControlManagementType = sourceControlManagementType;
        this.repositoryUrl = repositoryUrl;
        this.branchName = branchName;
        this.commitId = commitId;
    }

    public String getSourceControlManagementType() {
        return sourceControlManagementType;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getCommitId() {
        return commitId;
    }
}
