package io.jenkins.plugins.signpath.PipelineData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.plugins.signpath.PipelineData.Model.BuildDto;
import io.jenkins.plugins.signpath.PipelineData.Model.BuildSystemDto;
import io.jenkins.plugins.signpath.PipelineData.Model.CommitDto;
import io.jenkins.plugins.signpath.PipelineData.Model.DefinitionDto;
import io.jenkins.plugins.signpath.PipelineData.Model.OriginDto;
import io.jenkins.plugins.signpath.PipelineData.Model.PipelineDataDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SecurityAssertionsDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SourceCodeDto;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SIGN-8500. Verifies the JSON shape produced for a fully-populated PipelineDataDto
 * matches the SignPath C# contract: PascalCase property names, the underscore
 * prefix on {@code _version}, the all-false SecurityAssertions block, and
 * suppression of null optional fields.
 */
public class PipelineDataJsonSerializerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void toJson_producesContractFieldNames() throws Exception {
        PipelineDataDto dto = sampleDto();

        String json = PipelineDataJsonSerializer.toJson(dto);
        JsonNode root = MAPPER.readTree(json);

        assertEquals("1.0", root.get("_version").asText());
        assertTrue("expected Build object", root.has("Build"));
        assertTrue("expected SourceCode object", root.has("SourceCode"));

        JsonNode build = root.get("Build");
        assertTrue(build.has("Definition"));
        assertTrue(build.has("SecurityAssertions"));
        assertTrue(build.has("StartedAt"));
        assertTrue(build.has("System"));
        assertTrue(build.has("WebUrl"));

        JsonNode definition = build.get("Definition");
        assertEquals("https://example.com/acme/app.git", definition.get("Repository").asText());
        assertEquals("main", definition.get("Branch").asText());
        assertEquals("Jenkinsfile", definition.get("Path").asText());
        assertEquals("0123456789abcdef0123456789abcdef01234567", definition.get("Commit").get("Id").asText());
        assertFalse("WebUrl on Commit should be omitted when null",
                definition.get("Commit").has("WebUrl"));

        JsonNode assertions = build.get("SecurityAssertions");
        assertFalse(assertions.get("Ephemeral").asBoolean());
        assertFalse(assertions.get("NoAccessToPlatformSecrets").asBoolean());
        assertFalse(assertions.get("NoConcurrentJobsOnAgent").asBoolean());
        assertFalse(assertions.get("NoImplicitCaching").asBoolean());
        assertFalse(assertions.get("NoImplicitRemoteAccessToAgent").asBoolean());

        assertEquals("legacy-instance-id", build.get("System").get("Id").asText());

        JsonNode sourceCode = root.get("SourceCode");
        assertFalse("IsPublicRepository should be omitted when null",
                sourceCode.has("IsPublicRepository"));
        JsonNode origin = sourceCode.get("Origin");
        assertEquals("git", origin.get("Type").asText());
        assertEquals("https://example.com/acme/app.git", origin.get("Url").asText());
        assertEquals("feature/SIGN-8500", origin.get("Branch").asText());
        assertEquals("fedcba9876543210fedcba9876543210fedcba98", origin.get("Commit").get("Id").asText());
    }

    @Test
    public void toJson_omitsNullOptionalFields() throws Exception {
        // Definition omitted; WebUrl omitted; IsPublicRepository omitted.
        BuildDto build = new BuildDto(
                null,
                new SecurityAssertionsDto(),
                "2026-04-30T12:00:00Z",
                new BuildSystemDto("legacy-instance-id"),
                null);
        SourceCodeDto sc = new SourceCodeDto(new OriginDto(
                "git", "https://example.com/acme/app.git", "main",
                new CommitDto("fedcba9876543210fedcba9876543210fedcba98", null), null));
        PipelineDataDto dto = new PipelineDataDto(build, sc);

        String json = PipelineDataJsonSerializer.toJson(dto);
        JsonNode root = MAPPER.readTree(json);

        assertFalse("Build.Definition should be omitted when null", root.get("Build").has("Definition"));
        assertFalse("Build.WebUrl should be omitted when null", root.get("Build").has("WebUrl"));
        assertFalse("Origin.WebUrl should be omitted when null",
                root.get("SourceCode").get("Origin").has("WebUrl"));
        assertFalse("Origin.Commit.WebUrl should be omitted when null",
                root.get("SourceCode").get("Origin").get("Commit").has("WebUrl"));
    }

    private static PipelineDataDto sampleDto() {
        DefinitionDto definition = new DefinitionDto(
                "https://example.com/acme/app.git",
                "main",
                "Jenkinsfile",
                new CommitDto("0123456789abcdef0123456789abcdef01234567", null),
                null);
        BuildDto build = new BuildDto(
                definition,
                new SecurityAssertionsDto(),
                "2026-04-30T12:00:00Z",
                new BuildSystemDto("legacy-instance-id"),
                "https://jenkins.example/job/example/42/");
        OriginDto origin = new OriginDto(
                "git",
                "https://example.com/acme/app.git",
                "feature/SIGN-8500",
                new CommitDto("fedcba9876543210fedcba9876543210fedcba98", null),
                null);
        SourceCodeDto sourceCode = new SourceCodeDto(origin);
        return new PipelineDataDto(build, sourceCode);
    }
}
