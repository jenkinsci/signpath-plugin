package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataOriginDto (source-code origin).
 *
 * <p>Per the C# contract: {@code Type}, {@code Url}, {@code Branch}, {@code Commit}
 * are required; {@code WebUrl} is optional. {@code type} is a free-form SCM type
 * string (e.g. {@code "git"}).</p>
 *
 * <p>Distinct from {@link DefinitionDto} (build-definition origin) which carries a
 * {@code Repository} clone URL and a {@code Path} into the repo instead.</p>
 */
@JsonPropertyOrder({"Type", "Url", "Branch", "Commit", "WebUrl"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OriginDto {
    @JsonProperty("Type")
    private final String type;
    @JsonProperty("Url")
    private final String url;
    @JsonProperty("Branch")
    private final String branch;
    @JsonProperty("Commit")
    private final CommitDto commit;
    @JsonProperty("WebUrl")
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
