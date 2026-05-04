package io.jenkins.plugins.signpath.PipelineData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jenkins.plugins.signpath.PipelineData.Model.PipelineDataDto;

/**
 * SIGN-8500. Serializes a {@link PipelineDataDto} to the JSON shape SignPath expects.
 *
 * <p>Field names and ordering are pinned by the {@code @JsonProperty} /
 * {@code @JsonPropertyOrder} annotations on the DTO classes; they mirror the
 * authoritative C# {@code PipelineDataDto} including the underscore-prefixed
 * {@code _version}.</p>
 */
public final class PipelineDataJsonSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private PipelineDataJsonSerializer() {
    }

    /**
     * Returns the canonical (compact, no extra whitespace) JSON for the given DTO.
     */
    public static String toJson(PipelineDataDto dto) throws JsonProcessingException {
        return MAPPER.writeValueAsString(dto);
    }

    /**
     * Returns indented JSON suitable for log output.
     */
    public static String toPrettyJson(PipelineDataDto dto) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(dto);
    }
}
