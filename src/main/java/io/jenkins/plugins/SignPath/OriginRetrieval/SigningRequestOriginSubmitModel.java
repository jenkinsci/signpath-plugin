package io.jenkins.plugins.SignPath.OriginRetrieval;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

public class SigningRequestOriginSubmitModel {
    private RepositoryMetadataModel repositoryMetadata;
    private String buildUrl;
    private TemporaryFile buildSettingsFile;

    public SigningRequestOriginSubmitModel(RepositoryMetadataModel repositoryMetadata, String buildUrl, TemporaryFile buildSettingsFile) {
        this.repositoryMetadata = repositoryMetadata;
        this.buildUrl = buildUrl;
        this.buildSettingsFile = buildSettingsFile;
    }

    public RepositoryMetadataModel getRepositoryMetadata() {
        return repositoryMetadata;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public TemporaryFile getBuildSettingsFile() { return buildSettingsFile; }
}
