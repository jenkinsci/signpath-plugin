package io.jenkins.plugins.signpath;

import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import io.jenkins.plugins.signpath.StepShared.GetSignedArtifactStepInput;
import io.jenkins.plugins.signpath.StepShared.SignPathContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;

/**
 * The step-execution for the
 *
 * @see GetSignedArtifactStep
 */
public class GetSignedArtifactStepExecution extends SynchronousStepExecution<String> {
    private final PrintStream logger;
    private final SecretRetriever secretRetriever;
    private final ArtifactFileManager artifactFileManager;
    private final SignPathFacadeFactory signPathFacadeFactory;
    private final GetSignedArtifactStepInput input;

    protected GetSignedArtifactStepExecution(GetSignedArtifactStepInput input,
                                             SignPathContext signPathContext) {
        super(signPathContext.getStepContext());
        this.input = input;
        this.logger = signPathContext.getLogger();
        this.secretRetriever = signPathContext.getSecretRetriever();
        this.artifactFileManager = signPathContext.getArtifactFileManager();
        this.signPathFacadeFactory = signPathContext.getSignPathFacadeFactory();
    }

    @Override
    protected String run() throws SignPathStepFailedException {
        logger.printf("Downloading signed artifact for organization: %s and signingRequest: %s\n", input.getOrganizationId(), input.getSigningRequestId());

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            String ciUserToken = secretRetriever.retrieveSecret(input.getCiUserTokenCredentialId());
            SignPathCredentials credentials = new SignPathCredentials(ciUserToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), input.getSigningRequestId());

            artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
            logger.print("Downloading signed artifact succeeded\n");
            return "";
        } catch (SecretNotFoundException | SignPathFacadeCallException | IOException | InterruptedException | NoSuchAlgorithmException ex) {
            logger.print("Downloading signed artifact failed: " + ex.getMessage() + "\n");
            throw new SignPathStepFailedException("Downloading signed artifact failed: " + ex.getMessage(), ex);
        }
    }
}
