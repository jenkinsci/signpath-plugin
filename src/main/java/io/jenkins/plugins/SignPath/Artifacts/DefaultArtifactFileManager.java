package io.jenkins.plugins.SignPath.Artifacts;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.FingerprintMap;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.ArtifactNotFoundException;
import jenkins.model.ArtifactManager;
import jenkins.util.BuildListenerAdapter;
import jenkins.util.VirtualFile;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

/**
 * The default implementation of the
 *
 * @see ArtifactFileManager interface
 */
public class DefaultArtifactFileManager implements ArtifactFileManager {
    private FingerprintMap fingerprintMap;
    private final Run<?, ?> run;
    private final Launcher launcher;
    private final TaskListener listener;

    public DefaultArtifactFileManager(FingerprintMap fingerprintMap, Run<?, ?> run, Launcher launcher, TaskListener listener) {
        this.fingerprintMap = fingerprintMap;
        this.run = run;
        this.launcher = launcher;
        this.listener = listener;
    }

    @Override
    public TemporaryFile retrieveArtifact(String artifactPath) throws IOException, ArtifactNotFoundException {
        if(artifactPath.contains(".."))
            throw new IllegalAccessError("artifactPath cannot be in parent directory.");

        ArtifactManager artifactManager = run.getArtifactManager();

        VirtualFile artifactFile = artifactManager.root().child(artifactPath);
        if (!artifactFile.exists()) {
            throw new ArtifactNotFoundException(String.format("The artifact at path \"%s\" was not found.", artifactPath));
        }

        String fileName = getFileName(artifactPath);
        TemporaryFile temporaryArtifactFile = new TemporaryFile(fileName);
        try (InputStream in = artifactFile.open()) {
            temporaryArtifactFile.copyFrom(in);
        }
        return temporaryArtifactFile;
    }

    private String getFileName(String artifactPath) {
        String normalizedArtifactPath = artifactPath.replace("\\", "/");
        if (normalizedArtifactPath.contains("/")) {
            // TODO SIGN-3415: Slash at the end probably kills this
            return normalizedArtifactPath.substring(normalizedArtifactPath.lastIndexOf("/")+1);
        }

        return normalizedArtifactPath;
    }

    @Override
    public void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if(targetArtifactPath.contains(".."))
            throw new IllegalAccessError("targetArtifactPath cannot be in parent directory.");

        ArtifactManager artifactManager = run.getArtifactManager();

        // TODO SIGN-3415: Normalize could be a method and shared with above
        String normalizedArtifactPath = targetArtifactPath.replace("\\", "/");

        artifactManager.archive(
                new FilePath(artifact.getFile().getParentFile()),
                launcher,
                BuildListenerAdapter.wrap(listener),
                Collections.singletonMap(normalizedArtifactPath, artifact.getFile().getName()));

        // TODO SIGN-3415: Put into method which ddescribes what this does
        String targetFileName = getFileName(targetArtifactPath);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String digest = Util.toHexString(digest(artifact.getFile(), md5));
        fingerprintMap.getOrCreate(run, targetFileName, digest);
    }

    private static byte[] digest(final File file, MessageDigest algorithm) throws IOException {
        algorithm.reset();
        try (FileInputStream fis = new FileInputStream(file)) {
            try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                try (DigestInputStream dis = new DigestInputStream(bis, algorithm)) {
                    // TODO SIGN-3415: Google how to stream a whole stream.
                    while (dis.read() != -1) {
                    }
                    return algorithm.digest();
                }
            }
        }
    }
}