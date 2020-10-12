package io.jenkins.plugins.SignPath.StepShared;

import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.IPowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.SignPathPowerShellFacadeFactory;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Artifacts.IArtifactFileManager;
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.SignPath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.SignPath.OriginRetrieval.IOriginRetriever;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;
import java.io.PrintStream;

// REVIEW SIGN-3415: This class essentially represents a dependency-injection replacement - is this ok?
public class SignPathContext {
    private StepContext context;
    private final Run<?, ?> run;
    private final PrintStream logger;
    private final String jenkinsRootUrl;
    private final ISecretRetriever secretRetriever;
    private final IOriginRetriever originRetriever;
    private final IArtifactFileManager artifactFileManager;
    private final ISignPathFacadeFactory signPathFacadeFactory;

    private SignPathContext(StepContext context,
                            Run<?, ?> run,
                            PrintStream logger,
                            String jenkinsRootUrl,
                            ISecretRetriever secretRetriever,
                            IOriginRetriever originRetriever,
                            IArtifactFileManager artifactFileManager,
                            ISignPathFacadeFactory signPathFacadeFactory) {
        this.context = context;
        this.run = run;
        this.logger = logger;
        this.jenkinsRootUrl = jenkinsRootUrl;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
    }

    public StepContext getStepContext() {
        return context;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public PrintStream getLogger() {
        return logger;
    }

    public String getJenkinsRootUrl() {
        return jenkinsRootUrl;
    }

    public ISecretRetriever getSecretRetriever() {
        return secretRetriever;
    }

    public IOriginRetriever getOriginRetriever() {
        return originRetriever;
    }

    public IArtifactFileManager getArtifactFileManager() {
        return artifactFileManager;
    }

    public ISignPathFacadeFactory getSignPathFacadeFactory() {
        return signPathFacadeFactory;
    }

    public static SignPathContext CreateForStep(StepContext context, ApiConfiguration apiConfiguration)
            throws IOException, InterruptedException, SignPathStepInvalidArgumentException {
        TaskListener listener = context.get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = context.get(Run.class);
        Launcher launcher = context.get(Launcher.class);
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();
        JenkinsLocationConfiguration config = JenkinsLocationConfiguration.get();
        String jenkinsRootUrl = config.getUrl();

        // non-valid urls result in null value here
        if(jenkinsRootUrl == null || jenkinsRootUrl.isEmpty()) {
            throw new SignPathStepInvalidArgumentException("The configured jenkins root url " + jenkinsRootUrl + " is not valid.");
        }

        ISecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        IOriginRetriever originRetriever = new OriginRetriever(new DefaultConfigFileProvider(run), run, jenkinsRootUrl);
        IArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);
        IPowerShellExecutor pwsh = new PowerShellExecutor("pwsh");
        ISignPathFacadeFactory signPathFacadeFactory = new SignPathPowerShellFacadeFactory(pwsh, apiConfiguration);

        return new SignPathContext(context, run, logger, jenkinsRootUrl, secretRetriever, originRetriever, artifactFileManager, signPathFacadeFactory);
    }
}
