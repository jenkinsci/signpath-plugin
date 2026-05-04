package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataBuildSystemDto.
 * Required nested object on Build with a single required string {@code Id}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BuildSystemDto {
    @JsonProperty("Id")
    private final String id;

    public BuildSystemDto(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
