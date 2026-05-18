package io.jenkins.plugins.signpath.PipelineData.Model;

public final class BuildSystemDto {
    private final String id;

    public BuildSystemDto(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
