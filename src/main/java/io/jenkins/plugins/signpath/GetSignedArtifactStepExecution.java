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
import org.jenkinsci.plugins.workflow.steps.StepContext;
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
                                             SecretRetriever secretRetriever,
                                             ArtifactFileManager artifactFileManager,
                                             SignPathFacadeFactory signPathFacadeFactory,
                                             PrintStream logger,
                                             StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.logger = logger;
        this.secretRetriever = secretRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
    }

    @Override
    protected String run() throws SignPathStepFailedException {
        logger.printf("Downloading signed artifact for organization: %s and signingRequest: %s%n", input.getOrganizationId(), input.getSigningRequestId());

        // TODO SIGN-3498: Check if we can also specify "no result" (or maybe return the output artifact path?)

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            String ciUserToken = secretRetriever.retrieveSecret(input.getCiUserTokenCredentialId());
            SignPathCredentials credentials = new SignPathCredentials(ciUserToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            try(TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), input.getSigningRequestId())) {
                artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
                logger.println("Downloading signed artifact succeeded");
                return "";
            }
        } catch (SecretNotFoundException | SignPathFacadeCallException | IOException | InterruptedException | NoSuchAlgorithmException ex) {
            logger.printf("Downloading signed artifact failed %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Downloading signed artifact failed: " + ex.getMessage(), ex);
        }
    }
}
