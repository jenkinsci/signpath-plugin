package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataDefinitionDto (build-definition origin).
 *
 * <p>Per the C# contract: {@code Repository}, {@code Branch}, {@code Path}, {@code Commit}
 * are all required when present; {@code WebUrl} is optional and left null in v1 per
 * ticket boundaries.</p>
 *
 * <p>Distinct from {@link OriginDto} (source-code origin) which carries a {@code Type}
 * and {@code Url} instead of {@code Repository}/{@code Path}.</p>
 */
@JsonPropertyOrder({"Repository", "Branch", "Path", "Commit", "WebUrl"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DefinitionDto {
    @JsonProperty("Repository")
    private final String repository;
    @JsonProperty("Branch")
    private final String branch;
    @JsonProperty("Path")
    private final String path;
    @JsonProperty("Commit")
    private final CommitDto commit;
    @JsonProperty("WebUrl")
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
