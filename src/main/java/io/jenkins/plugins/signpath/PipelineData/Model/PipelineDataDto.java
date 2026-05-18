package io.jenkins.plugins.signpath.PipelineData.Model;

/**
 * Top-level mirror of SignPath's PipelineDataDto contract.
 */
public final class PipelineDataDto {
    public static final String SCHEMA_VERSION = "1.0";

    private final String version;
    private final BuildDto build;
    private final SourceCodeDto sourceCode;

    public PipelineDataDto(BuildDto build, SourceCodeDto sourceCode) {
        this.version = SCHEMA_VERSION;
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
