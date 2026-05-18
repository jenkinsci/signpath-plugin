package io.jenkins.plugins.signpath.PipelineData.Model;

public final class CommitDto {
    private final String id;
    private final String webUrl;

    public CommitDto(String id, String webUrl) {
        this.id = id;
        this.webUrl = webUrl;
    }

    public String getId() {
        return id;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
