package io.jenkins.plugins.SignPath.OriginRetrieval;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

public class SigningRequestOriginModel {
    private final RepositoryMetadataModel repositoryMetadata;
    private final String buildUrl;
    private final TemporaryFile buildSettingsFile;

    public SigningRequestOriginModel(RepositoryMetadataModel repositoryMetadata, String buildUrl, TemporaryFile buildSettingsFile) {
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
