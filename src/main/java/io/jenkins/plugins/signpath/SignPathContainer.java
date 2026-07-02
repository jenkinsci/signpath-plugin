package io.jenkins.plugins.signpath;

import hudson.Launcher;
import hudson.model.*;
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacadeFactory;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathClient.SignPathClientFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Artifacts.DefaultArtifactFileManager;
import io.jenkins.plugins.signpath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import io.signpath.signpathclient.SignPathClientSimpleLogger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;

/**
 * A helper class that helps us share all dependencies needed for the
 *
 * @see io.jenkins.plugins.signpath.SignPathStepBase
 * implementations
 */
public class SignPathContainer {
    private final StepContext stepContext;
    private final Run<?, ?> run;
    private final TaskListener taskListener;
    private final SecretRetriever secretRetriever;
    private final ArtifactFileManager artifactFileManager;
    private final PipelineConnectorFacadeFactory pipelineConnectorFacadeFactory;

    private SignPathContainer(StepContext stepContext,
                              Run<?, ?> run,
                              TaskListener taskListener,
                              SecretRetriever secretRetriever,
                              ArtifactFileManager artifactFileManager,
                              PipelineConnectorFacadeFactory pipelineConnectorFacadeFactory) {
        this.stepContext = stepContext;
        this.run = run;
        this.taskListener = taskListener;
        this.secretRetriever = secretRetriever;
        this.artifactFileManager = artifactFileManager;
        this.pipelineConnectorFacadeFactory = pipelineConnectorFacadeFactory;
    }

    public StepContext getStepContext() {
        return stepContext;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public TaskListener getTaskListener() {
        return taskListener;
    }

    public SecretRetriever getSecretRetriever() {
        return secretRetriever;
    }

    public ArtifactFileManager getArtifactFileManager() {
        return artifactFileManager;
    }

    public PipelineConnectorFacadeFactory getPipelineConnectorFacadeFactory() {
        return pipelineConnectorFacadeFactory;
    }

    public static SignPathContainer build(StepContext context, ApiConfiguration apiConfiguration)
            throws IOException, InterruptedException {
        TaskListener listener = context.get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = context.get(Run.class);
        Launcher launcher = context.get(Launcher.class);
        SignPathClientSimpleLogger logger = new SignPathClientLogger(listener.getLogger());
        Jenkins jenkins = Jenkins.get();
        FingerprintMap fingerprintMap = jenkins.getFingerprintMap();

        SecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        ArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);

        PipelineConnectorFacadeFactory pipelineConnectorFacadeFactory = new SignPathClientFacadeFactory(apiConfiguration, logger);

        return new SignPathContainer(context, run, listener, secretRetriever, artifactFileManager, pipelineConnectorFacadeFactory);
    }
}
