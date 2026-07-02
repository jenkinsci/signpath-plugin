package io.jenkins.plugins.signpath.Artifacts;

import hudson.FilePath;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import jenkins.model.ArtifactManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * A wrapper around the
 *
 * @see ArtifactManager from Jenkins that helps with retrieving files instead of streams
 */
public interface ArtifactFileManager {
    /**
     * Retrieves the given artifact from the Jenkins Master-Node
     *
     * @param artifactPath the artifact path in the "Jenkins-Format" i.e. abc/def/new.exe
     * @return the temporary file containing the artifact content
     * @throws IOException               occurs if something went wrong with creating the temporary fiel
     * @throws ArtifactNotFoundException occurs if there was no artifact found at the given path
     */
    TemporaryFile retrieveArtifact(String artifactPath) throws IOException, ArtifactNotFoundException;

    /**
     * Stores the given artifact from the temporary file on the Jenkins Master-Node
     *
     * @param artifact           the artifact to store on Jenkins
     * @param targetArtifactPath the target path of the artifact
     * @throws IOException          occurs if something goes wrong with storing the artifact on Jenkins
     * @throws InterruptedException occurs if something goes wrong with storing the artifact on Jenkins
     * @throws NoSuchAlgorithmException occurs if something goes wrong with storing the artifact on Jenkins
     */
    void storeArtifact(TemporaryFile artifact, String targetArtifactPath) throws IOException, InterruptedException, NoSuchAlgorithmException;

    /**
     * Archives a file that lives in the agent workspace, streaming it from the agent to the build's
     * archived artifacts (the same mechanism as the built-in <code>archiveArtifacts</code> step) so it can
     * be retrieved later, e.g. by the SignPath Pipeline Connector.
     *
     * @param workspace    the agent workspace root
     * @param relativePath the artifact path relative to the workspace root; also used as the archived path
     * @throws IOException          occurs if something goes wrong while archiving
     * @throws InterruptedException occurs if the archiving is interrupted
     */
    void archiveWorkspaceArtifact(FilePath workspace, String relativePath) throws IOException, InterruptedException;
}
