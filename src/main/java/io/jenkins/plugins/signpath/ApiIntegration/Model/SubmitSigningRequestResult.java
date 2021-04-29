package io.jenkins.plugins.signpath.ApiIntegration.Model;

import io.jenkins.plugins.signpath.Common.TemporaryFile;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public class SubmitSigningRequestResult implements Closeable {

    private final TemporaryFile signedArtifact;
    private final UUID signingRequestId;

    public SubmitSigningRequestResult(TemporaryFile signedArtifact, UUID signingRequestId) {
        this.signedArtifact = signedArtifact;
        this.signingRequestId = signingRequestId;
    }

    public TemporaryFile getSignedArtifact() {
        return signedArtifact;
    }

    public UUID getSigningRequestId() {
        return signingRequestId;
    }

    @Override
    public void close() {
        signedArtifact.close();
    }
}
