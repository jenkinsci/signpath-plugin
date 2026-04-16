package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

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
    private transient final SignPathFacadeFactory signPathFacadeFactory;
    private transient final TaskListener taskListener;

    protected GetSignedArtifactStepExecution(GetSignedArtifactStepInput input,
                                             SecretRetriever secretRetriever,
                                             SignPathFacadeFactory signPathFacadeFactory,
                                             TaskListener taskListener,
                                             StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.secretRetriever = secretRetriever;
        this.signPathFacadeFactory = signPathFacadeFactory;
        this.taskListener = taskListener;
    }

    @Override
    protected Void run() throws SignPathStepFailedException {
        PrintStream logger = taskListener.getLogger();

        logger.printf("Downloading signed artifact for organization: %s and signingRequest: %s%n", input.getOrganizationId(), input.getSigningRequestId());

        try {
            FilePath workspace = getContext().get(FilePath.class);
            if (workspace == null) {
                throw new IOException("Could not obtain workspace from step context.");
            }

            Secret trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            Secret apiToken = secretRetriever.retrieveSecret(input.getApiTokenCredentialId(), new CredentialsScope[] { CredentialsScope.SYSTEM, CredentialsScope.GLOBAL });
            SignPathCredentials credentials = new SignPathCredentials(apiToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            // signedArtifact is a temporary download buffer on the controller, the try block ensures it is
            // cleaned up after its contents are copied to the persistent workspace file at outputArtifactPath.
            try (TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), input.getSigningRequestId())) {
                FilePath outputPath = workspace.child(input.getOutputArtifactPath());
                try (InputStream in = new FileInputStream(signedArtifact.getFile())) {
                    outputPath.copyFrom(in);
                }
                logger.println("Downloading signed artifact succeeded");
            }
        } catch (SecretNotFoundException | SignPathFacadeCallException | IOException | InterruptedException ex) {
            logger.printf("Downloading signed artifact failed %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Downloading signed artifact failed: " + ex.getMessage(), ex);
        }

        return null; // Void in java is just a placeholder-class for generics where we don't want a return
    }
}
