package io.jenkins.plugins.signpath.Artifacts;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.FingerprintMap;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import jenkins.model.ArtifactManager;
import jenkins.util.BuildListenerAdapter;
import jenkins.util.VirtualFile;
import org.apache.commons.lang.StringUtils;

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
    private final FingerprintMap fingerprintMap;
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
            throw new ArtifactNotFoundException(String.format("The artifact at path '%s' was not found.", artifactPath));
        }

        String fileName = getFileName(artifactPath);
        TemporaryFile temporaryArtifactFile = new TemporaryFile(fileName);
        try (InputStream in = artifactFile.open()) {
            temporaryArtifactFile.copyFrom(in);
        }
        return temporaryArtifactFile;
    }

    @Override
    public void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if(targetArtifactPath.contains(".."))
            throw new IllegalAccessError("targetArtifactPath cannot be in parent directory.");

        ArtifactManager artifactManager = run.getArtifactManager();

        String normalizedArtifactPath = getNormalizedPath(targetArtifactPath);
        artifactManager.archive(
                new FilePath(artifact.getFile().getParentFile()),
                launcher,
                BuildListenerAdapter.wrap(listener),
                Collections.singletonMap(normalizedArtifactPath, artifact.getFile().getName()));

        createFingerprint(artifact, targetArtifactPath);
    }

    private String getFileName(String artifactPath) {
        String normalizedArtifactPath = getNormalizedPath(artifactPath);
        if (normalizedArtifactPath.contains("/")) {
            return normalizedArtifactPath.substring(normalizedArtifactPath.lastIndexOf("/") + 1);
        }

        return normalizedArtifactPath;
    }

    private String getNormalizedPath(String artifactPath){
        return StringUtils.strip(artifactPath.replace("\\", "/"), "/");
    }

    private void createFingerprint(TemporaryFile artifact, String targetArtifactPath) throws NoSuchAlgorithmException, IOException {
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
                    // This is the best way to read a DigestInputStream in Java according to our current knowledge
                    //noinspection StatementWithEmptyBody
                    while (dis.read() != -1) {
                    }
                    return algorithm.digest();
                }
            }
        }
    }
}