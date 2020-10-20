package io.jenkins.plugins.SignPath.ApiIntegration.Model;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

/**
 * This class holds all the relevant "origin" information that is transferred to SignPath
 * This information should help you identify from which exact source a signing-request originated.
 */
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

    public TemporaryFile getBuildSettingsFile() {
        return buildSettingsFile;
    }
}