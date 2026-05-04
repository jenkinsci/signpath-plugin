package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataCommitDto.
 *
 * <p>Schema: {@code { Id (required), WebUrl (optional) }}. {@code webUrl} is
 * intentionally left null in v1 per ticket boundaries.</p>
 */
@JsonPropertyOrder({"Id", "WebUrl"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CommitDto {
    @JsonProperty("Id")
    private final String id;
    @JsonProperty("WebUrl")
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
