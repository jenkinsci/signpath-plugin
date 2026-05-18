package io.jenkins.plugins.signpath.PipelineData.Model;

public final class BuildDto {
    private final DefinitionDto definition;
    private final SecurityAssertionsDto securityAssertions;
    private final String startedAt;
    private final BuildSystemDto system;
    private final String webUrl;

    public BuildDto(DefinitionDto definition,
                    SecurityAssertionsDto securityAssertions,
                    String startedAt,
                    BuildSystemDto system,
                    String webUrl) {
        this.definition = definition;
        this.securityAssertions = securityAssertions;
        this.startedAt = startedAt;
        this.system = system;
        this.webUrl = webUrl;
    }

    public DefinitionDto getDefinition() {
        return definition;
    }

    public SecurityAssertionsDto getSecurityAssertions() {
        return securityAssertions;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public BuildSystemDto getSystem() {
        return system;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
