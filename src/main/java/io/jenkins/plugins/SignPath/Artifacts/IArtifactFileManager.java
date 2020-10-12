package io.jenkins.plugins.SignPath.Artifacts;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.ArtifactNotFoundException;

import java.io.IOException;

public interface IArtifactFileManager {
    TemporaryFile retrieveArtifact(String artifactPath) throws IOException, ArtifactNotFoundException;

    void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException;
}
