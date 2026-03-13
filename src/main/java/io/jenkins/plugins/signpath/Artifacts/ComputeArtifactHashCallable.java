package io.jenkins.plugins.signpath.Artifacts;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.apache.commons.codec.digest.DigestUtils;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A {@link FilePath.FileCallable} that runs on the agent and computes the SHA-256 hex hash
 * of the artifact file. The result is a 64-character lowercase hex string.
 */
public class ComputeArtifactHashCallable implements FilePath.FileCallable<String> {
    private static final long serialVersionUID = 1L;

    @Override
    public void checkRoles(RoleChecker checker) {
        // No role restrictions: this callable only reads file content and computes a hash
    }

    @Override
    public String invoke(File artifact, VirtualChannel channel) throws IOException {
        try (FileInputStream fis = new FileInputStream(artifact)) {
            return DigestUtils.sha256Hex(fis);
        }
    }
}
