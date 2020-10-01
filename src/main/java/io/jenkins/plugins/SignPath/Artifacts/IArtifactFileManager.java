package io.jenkins.plugins.SignPath.Artifacts;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.io.IOException;

public interface IArtifactFileManager {
    TemporaryFile retrieveArtifact(String artifactPath) throws IOException;

    void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException;
}
