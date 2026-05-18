package io.jenkins.plugins.signpath.PipelineData.Model;

public final class DefinitionDto {
    private final String repository;
    private final String branch;
    private final String path;
    private final CommitDto commit;
    private final String webUrl;

    public DefinitionDto(String repository, String branch, String path, CommitDto commit, String webUrl) {
        this.repository = repository;
        this.branch = branch;
        this.path = path;
        this.commit = commit;
        this.webUrl = webUrl;
    }

    public String getRepository() {
        return repository;
    }

    public String getBranch() {
        return branch;
    }

    public String getPath() {
        return path;
    }

    public CommitDto getCommit() {
        return commit;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
