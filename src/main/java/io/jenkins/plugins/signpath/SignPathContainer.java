package io.jenkins.plugins.signpath;

import hudson.Launcher;
import hudson.model.*;
import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathClient.SignPathClientFacadeFactory;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Artifacts.DefaultArtifactFileManager;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.signpath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.signpath.OriginRetrieval.GitOriginRetriever;
import io.jenkins.plugins.signpath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import io.signpath.signpathclient.SignPathClientSimpleLogger;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;

/**
 * A helper class that helps us share all dependencies needed for the
 *
 * @see io.jenkins.plugins.signpath.SignPathStepBase
 * implementations
 */
public class SignPathContainer {
    public static final String POWERSHELL_EXECUTABLE_NAME = "SignPath_PowerShellExecutableName";

    private final StepContext stepContext;
    private final Run<?, ?> run;
    private final TaskListener taskListener;
    private final SecretRetriever secretRetriever;
    private final OriginRetriever originRetriever;
    private final ArtifactFileManager artifactFileManager;
    private final SignPathFacadeFactory signPathFacadeFactory;

    private SignPathContainer(StepContext stepContext,
                              Run<?, ?> run,
                              TaskListener taskListener,
                              SecretRetriever secretRetriever,
                              OriginRetriever originRetriever,
                              ArtifactFileManager artifactFileManager,
                              SignPathFacadeFactory signPathFacadeFactory) {
        this.stepContext = stepContext;
        this.run = run;
        this.taskListener = taskListener;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
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

    public OriginRetriever getOriginRetriever() {
        return originRetriever;
    }

    public ArtifactFileManager getArtifactFileManager() {
        return artifactFileManager;
    }

    public SignPathFacadeFactory getSignPathFacadeFactory() {
        return signPathFacadeFactory;
    }

    public static SignPathContainer build(StepContext context, ApiConfiguration apiConfiguration)
            throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        TaskListener listener = context.get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = context.get(Run.class);
        Launcher launcher = context.get(Launcher.class);
        SignPathClientSimpleLogger logger = new SignPathClientLogger(listener.getLogger());
        Jenkins jenkins = Jenkins.get();
        FingerprintMap fingerprintMap = jenkins.getFingerprintMap();
        JenkinsLocationConfiguration config = JenkinsLocationConfiguration.get();
        String jenkinsRootUrl = config.getUrl();

        // non-valid urls result in null value here
        if (jenkinsRootUrl == null || jenkinsRootUrl.isEmpty()) {
            throw new SignPathStepInvalidArgumentException("The configured jenkins root url " + jenkinsRootUrl + " is not valid.");
        }

        SecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        OriginRetriever originRetriever = new GitOriginRetriever(new DefaultConfigFileProvider(run), run, jenkinsRootUrl);
        ArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);

        SignPathFacadeFactory signPathFacadeFactory = new SignPathClientFacadeFactory(apiConfiguration, logger);

        return new SignPathContainer(context, run, listener, secretRetriever, originRetriever, artifactFileManager, signPathFacadeFactory);
    }
}
