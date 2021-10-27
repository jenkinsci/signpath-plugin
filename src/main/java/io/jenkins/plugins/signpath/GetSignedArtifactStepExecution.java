package io.jenkins.plugins.signpath;

import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;

/**
 * The step-execution for the
 *
 * @see GetSignedArtifactStep
 */
public class GetSignedArtifactStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    // We do not support resuming execution and therefore can mark our fields as transient (=> not serialized)
    // If we want to support resuming, we need to remove 'transient' and make sure everything is serializable
    private transient final GetSignedArtifactStepInput input;
    private transient final SecretRetriever secretRetriever;
    private transient final ArtifactFileManager artifactFileManager;
    private transient final SignPathFacadeFactory signPathFacadeFactory;
    private transient final TaskListener taskListener;

    protected GetSignedArtifactStepExecution(GetSignedArtifactStepInput input,
                                             SecretRetriever secretRetriever,
                                             ArtifactFileManager artifactFileManager,
                                             SignPathFacadeFactory signPathFacadeFactory,
                                             TaskListener taskListener,
                                             StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.secretRetriever = secretRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
        this.taskListener = taskListener;
    }

    @Override
    protected Void run() throws SignPathStepFailedException {
        PrintStream logger = taskListener.getLogger();

        logger.printf("Downloading signed artifact for organization: %s and signingRequest: %s%n", input.getOrganizationId(), input.getSigningRequestId());

        try {
            Secret trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            Secret ciUserToken = secretRetriever.retrieveSecret(input.getCiUserTokenCredentialId());
            SignPathCredentials credentials = new SignPathCredentials(ciUserToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            try (TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), input.getSigningRequestId())) {
                artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
                logger.println("Downloading signed artifact succeeded");
            }
        } catch (SecretNotFoundException | SignPathFacadeCallException | IOException | InterruptedException | NoSuchAlgorithmException ex) {
            logger.printf("Downloading signed artifact failed %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Downloading signed artifact failed: " + ex.getMessage(), ex);
        }

        return null; // Void in java is just a placeholder-class for generics where we don't want a return
    }
}
