package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataSourceCodeDto.
 *
 * <p>Per ticket boundaries:</p>
 * <ul>
 *   <li>{@code origin}: REQUIRED on the SignPath side. The collector populates it only
 *       when the build had exactly one runtime checkout that is a GitSCM with a non-null
 *       BuildData. When that rule fires "skip", the collector returns a null
 *       PipelineDataDto + skip reasons rather than emitting an invalid DTO.</li>
 *   <li>{@code isPublicRepository}, {@code scmSystem}, {@code scmSystemData}: unset in v1.</li>
 * </ul>
 */
@JsonPropertyOrder({"IsPublicRepository", "Origin"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SourceCodeDto {
    @JsonProperty("IsPublicRepository")
    private final Boolean isPublicRepository;
    @JsonProperty("Origin")
    private final OriginDto origin;

    public SourceCodeDto(OriginDto origin) {
        this(null, origin);
    }

    public SourceCodeDto(Boolean isPublicRepository, OriginDto origin) {
        this.isPublicRepository = isPublicRepository;
        this.origin = origin;
    }

    public Boolean getIsPublicRepository() {
        return isPublicRepository;
    }

    public OriginDto getOrigin() {
        return origin;
    }
}
