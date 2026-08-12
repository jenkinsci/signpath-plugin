package io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacade;

import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.Exceptions.PipelineConnectorFacadeCallException;
import io.jenkins.plugins.signpath.SignPathClientLogger;
import io.jenkins.plugins.signpath.TestUtils.SignPathJenkinsRule;
import io.jenkins.plugins.signpath.TestUtils.Some;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PipelineConnectorFacadeTest {

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Test
    public void uploadUnsignedArtifact_withNonExistingFile_throws() throws Exception {
        SignPathCredentials credentials = new SignPathCredentials(Secret.fromString(Some.stringNonEmpty()));
        PipelineConnectorFacade sut = new PipelineConnectorFacade(credentials, Some.apiConfiguration(), new SignPathClientLogger(System.out));

        UUID organizationId = Some.uuid();
        UUID signingRequestId = Some.uuid();
        File nonExistingArtifact = new File(System.getProperty("java.io.tmpdir"), Some.stringNonEmpty() + ".exe");

        // ACT
        ThrowingRunnable act = () -> sut.uploadUnsignedArtifact(organizationId, signingRequestId, nonExistingArtifact);

        // ASSERT
        Throwable ex = assertThrows(PipelineConnectorFacadeCallException.class, act);
        assertTrue(ex.getMessage(), ex.getMessage().contains("does not exist"));
    }
}
