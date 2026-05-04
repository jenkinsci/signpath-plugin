package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataBuildDto.
 *
 * <ul>
 *   <li>{@code definition}: optional ({@link DefinitionDto}).</li>
 *   <li>{@code securityAssertions}: required, all-false in v1 per ticket.</li>
 *   <li>{@code startedAt}: required, RFC3339 UTC of {@code Run.getStartTimeInMillis()}.</li>
 *   <li>{@code system}: required, {@link BuildSystemDto} carrying Jenkins legacy instance ID.</li>
 *   <li>{@code webUrl}: optional, {@code rootUrl + run.getUrl()}.</li>
 *   <li>{@code systemData}: unset in v1 per ticket (would be a free-form JSON object).</li>
 * </ul>
 */
@JsonPropertyOrder({"Definition", "SecurityAssertions", "StartedAt", "System", "WebUrl", "SystemData"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BuildDto {
    @JsonProperty("Definition")
    private final DefinitionDto definition;
    @JsonProperty("SecurityAssertions")
    private final SecurityAssertionsDto securityAssertions;
    @JsonProperty("StartedAt")
    private final String startedAt;
    @JsonProperty("System")
    private final BuildSystemDto system;
    @JsonProperty("WebUrl")
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
