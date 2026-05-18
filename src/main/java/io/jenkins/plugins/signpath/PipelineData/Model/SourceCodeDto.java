package io.jenkins.plugins.signpath.PipelineData.Model;

public final class SourceCodeDto {
    private final Boolean isPublicRepository;
    private final OriginDto origin;
    private final ScmSystemDto scmSystem;

    public SourceCodeDto(OriginDto origin) {
        this(null, origin, null);
    }

    public SourceCodeDto(Boolean isPublicRepository, OriginDto origin, ScmSystemDto scmSystem) {
        this.isPublicRepository = isPublicRepository;
        this.origin = origin;
        this.scmSystem = scmSystem;
    }

    public Boolean getIsPublicRepository() {
        return isPublicRepository;
    }

    public OriginDto getOrigin() {
        return origin;
    }

    public ScmSystemDto getScmSystem() {
        return scmSystem;
    }
}
