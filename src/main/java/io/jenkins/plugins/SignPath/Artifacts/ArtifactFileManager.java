package io.jenkins.plugins.SignPath.Artifacts;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import jenkins.model.ArtifactManager;
import jenkins.util.BuildListenerAdapter;
import jenkins.util.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

// TODO SIGN-3326: Probably add fingerprinting as well.
public class ArtifactFileManager implements IArtifactFileManager {
    private final Run<?, ?> run;
    private final Launcher launcher;
    private final TaskListener listener;

    public ArtifactFileManager(Run<?, ?> run, Launcher launcher, TaskListener listener){
        this.run = run;
        this.launcher = launcher;
        this.listener = listener;
    }

    @Override
    public TemporaryFile retrieveArtifact(String artifactPath) throws IOException {
        ArtifactManager artifactManager = run.getArtifactManager();
        VirtualFile artifactFile = artifactManager.root().child(artifactPath);
        if (!artifactFile.exists()) {
            throw new IllegalArgumentException("artifact file does not exist");
        }

        TemporaryFile temporaryArtifactFile = new TemporaryFile();
        try (InputStream in = artifactFile.open()) {
            temporaryArtifactFile.copyFrom(in);
        }
        return temporaryArtifactFile;
    }

    @Override
    public void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException {
        ArtifactManager artifactManager = run.getArtifactManager();

        artifactManager.archive(
                new FilePath(artifact.getFile().getParentFile()),
                launcher,
                BuildListenerAdapter.wrap(listener),
                Collections.singletonMap(targetArtifactPath, artifact.getFile().getName()));
    }
}
