package io.jenkins.plugins.signpath.PipelineData;

import hudson.plugins.git.Branch;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the pure helpers on {@link PipelineDataCollector}. The
 * collector's main entry point relies on Jenkins types ({@code WorkflowRun},
 * {@code BuildData}, {@code SCMRevisionAction}) that need a Jenkins runtime,
 * so end-to-end coverage lives in {@code SubmitSigningRequestStepEndToEndTest}.
 */
public class PipelineDataCollectorTest {

    @Test
    public void rfc3339UtcRendersInstantInIsoFormat() {
        long epochMillis = Instant.parse("2026-05-12T13:00:00Z").toEpochMilli();
        assertEquals("2026-05-12T13:00:00Z", PipelineDataCollector.rfc3339Utc(epochMillis));
    }

    @Test
    public void rfc3339UtcIsUtcRegardlessOfTimeZone() {
        // A non-round-second value should still round-trip through ISO-8601.
        long epochMillis = Instant.parse("2024-01-15T08:30:45.123Z").toEpochMilli();
        assertEquals("2024-01-15T08:30:45.123Z", PipelineDataCollector.rfc3339Utc(epochMillis));
    }

    @Test
    public void pickFirstBranchRefSkipsTagsAndPicksFirstRefsRemotesEntry() {
        List<Branch> branches = Arrays.asList(
                new Branch("refs/tags/v1.0", ObjectId.fromString("0000000000000000000000000000000000000000")),
                new Branch("refs/remotes/origin/feature/x", ObjectId.fromString("0000000000000000000000000000000000000000")),
                new Branch("refs/remotes/origin/main", ObjectId.fromString("0000000000000000000000000000000000000000")));

        assertEquals("refs/remotes/origin/feature/x", PipelineDataCollector.pickFirstBranchRef(branches));
    }

    @Test
    public void pickFirstBranchRefReturnsNullWhenOnlyTagRefsPresent() {
        List<Branch> branches = Arrays.asList(
                new Branch("refs/tags/v1.0", ObjectId.fromString("0000000000000000000000000000000000000000")),
                new Branch("refs/tags/v2.0", ObjectId.fromString("0000000000000000000000000000000000000000")));

        assertNull(PipelineDataCollector.pickFirstBranchRef(branches));
    }

    @Test
    public void pickFirstBranchRefReturnsNullForEmptyCollection() {
        assertNull(PipelineDataCollector.pickFirstBranchRef(Collections.<Branch>emptyList()));
    }
}
