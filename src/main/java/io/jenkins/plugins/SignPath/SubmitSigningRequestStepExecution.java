package io.jenkins.plugins.SignPath;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.OriginRetrieval.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.PrintStream;

public class SubmitSigningRequestStepExecution extends SynchronousStepExecution {

    private SubmitSigningRequestStep signStep;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStep signStep, StepContext context) {
        super(context);
        this.signStep = signStep;
    }

    @Override
    protected String run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        Run run = getContext().get(Run.class);
        Launcher launcher = getContext().get(Launcher.class);
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootUrl = jenkins.getConfiguredRootUrl();

        logger.println("signArtifact organizationId:" + signStep.getOrganizationId() + " waitForCompletion: " + signStep.getWaitForCompletion());

        // TODO SIGN-3326: Add to dependency-injection
        CredentialBasedSecretRetriever credentialSecretRetriever = new CredentialBasedSecretRetriever(jenkins);
        OriginRetriever originRetriever = new OriginRetriever(new DefaultConfigFileProvider(run), run, jenkinsRootUrl);
        ArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);

        String trustedBuildSystemToken = credentialSecretRetriever.retrieveSecret("TrustedBuildSystemToken");
        SigningRequestOriginModel originModel = originRetriever.retrieveOrigin();
        TemporaryFile unsignedArtifact = artifactFileManager.retrieveArtifact("Calculator\\bin\\Release\\netcoreapp3.1\\publish\\Calculator.deps.json");

        try (PowerShell powerShell = PowerShell.openSession("pwsh.exe")) {
            PowerShellResponse response = powerShell.executeSingleCommand("Get-Process");
            logger.println("Get-Process Result:" + response.getCommandOutput());
        }

        return "something";
    }
}
