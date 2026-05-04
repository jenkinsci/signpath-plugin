package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Top-level mirror of SignPath's PipelineDataDto.
 *
 * <p>The {@code _version} property is required by the SignPath contract. Per the
 * authoritative C# DTO it uses an underscore-prefixed JSON name. SIGN-8500 spike
 * confirmed the value is hardcoded to {@code "1.0"}; bumping the schema requires
 * a coordinated SignPath-side change.</p>
 */
@JsonPropertyOrder({"_version", "Build", "SourceCode"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PipelineDataDto {
    public static final String SCHEMA_VERSION = "1.0";

    @JsonProperty("_version")
    private final String version;
    @JsonProperty("Build")
    private final BuildDto build;
    @JsonProperty("SourceCode")
    private final SourceCodeDto sourceCode;

    public PipelineDataDto(BuildDto build, SourceCodeDto sourceCode) {
        this(SCHEMA_VERSION, build, sourceCode);
    }

    public PipelineDataDto(String version, BuildDto build, SourceCodeDto sourceCode) {
        this.version = version;
        this.build = build;
        this.sourceCode = sourceCode;
    }

    public String getVersion() {
        return version;
    }

    public BuildDto getBuild() {
        return build;
    }

    public SourceCodeDto getSourceCode() {
        return sourceCode;
    }
}
