package io.jenkins.plugins.SignPath.OriginRetrieval;

public class SigningRequestOriginSubmitModel {
    private RepositoryMetadataModel repositoryMetadata;
    private String buildUrl;

    // TODO SIGN-3326: Add BuildSettingsFile? What should it be in Jenkins

    public SigningRequestOriginSubmitModel(RepositoryMetadataModel repositoryMetadata, String buildUrl) {
        this.repositoryMetadata = repositoryMetadata;
        this.buildUrl = buildUrl;
    }

    public RepositoryMetadataModel getRepositoryMetadata() {
        return repositoryMetadata;
    }

    public String getBuildUrl() {
        return buildUrl;
    }
}
