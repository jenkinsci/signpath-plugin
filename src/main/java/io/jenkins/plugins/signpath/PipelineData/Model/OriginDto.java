package io.jenkins.plugins.signpath.PipelineData.Model;

public final class OriginDto {
    private final String type;
    private final String url;
    private final String branch;
    private final CommitDto commit;
    private final String webUrl;

    public OriginDto(String type, String url, String branch, CommitDto commit, String webUrl) {
        this.type = type;
        this.url = url;
        this.branch = branch;
        this.commit = commit;
        this.webUrl = webUrl;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getBranch() {
        return branch;
    }

    public CommitDto getCommit() {
        return commit;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
