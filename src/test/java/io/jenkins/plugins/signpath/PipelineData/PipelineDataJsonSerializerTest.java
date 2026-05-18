package io.jenkins.plugins.signpath.PipelineData;

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

public class PipelineDataJsonSerializerTest {

    @Test
    public void serializesMultibranchPipelineDataInExpectedShape() {
        DefinitionDto definition = new DefinitionDto(
                "https://github.com/example/repo.git",
                "main",
                "Jenkinsfile",
                new CommitDto("abc123", null),
                null);
        BuildDto build = new BuildDto(
                definition,
                new SecurityAssertionsDto(),
                "2026-05-12T13:00:00Z",
                new BuildSystemDto("jenkins"),
                "https://jenkins.example/job/x/1/");
        OriginDto origin = new OriginDto(
                "git",
                "https://github.com/example/repo.git",
                "main",
                new CommitDto("abc123", null),
                null);
        PipelineDataDto dto = new PipelineDataDto(build, new SourceCodeDto(origin));

        String json = PipelineDataJsonSerializer.toJson(dto);

        assertContains(json, "\"_version\":\"1.0\"");
        assertContains(json, "\"Build\":{");
        assertContains(json, "\"Definition\":{");
        assertContains(json, "\"Repository\":\"https://github.com/example/repo.git\"");
        assertContains(json, "\"Branch\":\"main\"");
        assertContains(json, "\"Path\":\"Jenkinsfile\"");
        assertContains(json, "\"Commit\":{\"Id\":\"abc123\"}");
        assertContains(json, "\"SecurityAssertions\":{");
        assertContains(json, "\"Ephemeral\":false");
        assertContains(json, "\"NoAccessToPlatformSecrets\":false");
        assertContains(json, "\"NoConcurrentJobsOnAgent\":false");
        assertContains(json, "\"NoImplicitCaching\":false");
        assertContains(json, "\"NoImplicitRemoteAccessToAgent\":false");
        assertContains(json, "\"StartedAt\":\"2026-05-12T13:00:00Z\"");
        assertContains(json, "\"System\":{\"Id\":\"jenkins\"}");
        assertContains(json, "\"WebUrl\":\"https://jenkins.example/job/x/1/\"");
        assertContains(json, "\"SourceCode\":{");
        assertContains(json, "\"Origin\":{");
        assertContains(json, "\"Type\":\"git\"");
        // No accidental nulls.
        assertFalse("JSON must not contain raw 'null' values: " + json, json.contains(":null"));
    }

    @Test
    public void omitsNullDefinitionAndOptionalWebUrls() {
        BuildDto build = new BuildDto(
                null, // Definition omitted (Cps*FlowDefinition shape)
                new SecurityAssertionsDto(),
                "2026-05-12T13:00:00Z",
                new BuildSystemDto("jenkins"),
                null);
        OriginDto origin = new OriginDto(
                "git",
                "https://github.com/example/repo.git",
                "refs/remotes/origin/main",
                new CommitDto("deadbeef", null),
                null);
        PipelineDataDto dto = new PipelineDataDto(build, new SourceCodeDto(origin));

        String json = PipelineDataJsonSerializer.toJson(dto);

        assertFalse(json.contains("\"Definition\""));
        assertFalse(json.contains("\"WebUrl\""));
        assertContains(json, "\"Branch\":\"refs/remotes/origin/main\"");
        assertContains(json, "\"Id\":\"deadbeef\"");
    }

    @Test
    public void escapesSpecialCharactersInStringValues() {
        BuildDto build = new BuildDto(
                null,
                new SecurityAssertionsDto(),
                "2026-05-12T13:00:00Z",
                new BuildSystemDto("jenkins"),
                "https://example.com/\"with\"\\quotes\nand newline");
        OriginDto origin = new OriginDto("git", "https://x", "refs/remotes/origin/main",
                new CommitDto("abc", null), null);
        PipelineDataDto dto = new PipelineDataDto(build, new SourceCodeDto(origin));

        String json = PipelineDataJsonSerializer.toJson(dto);

        assertContains(json, "\\\"with\\\"");
        assertContains(json, "\\\\quotes");
        assertContains(json, "\\nand");
    }

    @Test
    public void schemaVersionIsFixedAtOneDotZero() {
        assertEquals("1.0", PipelineDataDto.SCHEMA_VERSION);
    }

    private static void assertContains(String haystack, String needle) {
        assertTrue("Expected JSON to contain " + needle + " but was: " + haystack, haystack.contains(needle));
    }
}
