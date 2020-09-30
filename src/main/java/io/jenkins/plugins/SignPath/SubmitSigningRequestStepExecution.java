package io.jenkins.plugins.SignPath;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.OriginRetrieval.SigningRequestOriginSubmitModel;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import jenkins.model.ArtifactManager;
import jenkins.model.Jenkins;
import jenkins.util.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootUrl = jenkins.getConfiguredRootUrl();

        logger.println("signArtifact organizationId:" + signStep.getOrganizationId() + " waitForCompletion: " + signStep.getWaitForCompletion());

        // TODO SIGN-3326: Add to dependency-injection
        CredentialBasedSecretRetriever credentialSecretRetriever = new CredentialBasedSecretRetriever(jenkins);
        OriginRetriever originRetriever = new OriginRetriever(new DefaultConfigFileProvider());

        String trustedBuildSystemToken = credentialSecretRetriever.retrieveSecret("TrustedBuildSystemToken");
        SigningRequestOriginSubmitModel originSubmitModel = originRetriever.retrieveForBuild(jenkinsRootUrl, run);

        ArtifactManager artifactManager = run.getArtifactManager();
        VirtualFile unsignedArtifact = artifactManager.root().child("Calculator\\bin\\Release\\netcoreapp3.1\\publish\\Calculator.deps.json");
        if (!unsignedArtifact.exists()) {
            throw new IllegalArgumentException("artifact file does not exist");
        }

        try (InputStream s = unsignedArtifact.open()) {
            String content = IOUtils.toString(s, StandardCharsets.UTF_8);
            logger.println(content);
        }

        try (PowerShell powerShell = PowerShell.openSession("pwsh.exe")) {
            PowerShellResponse response = powerShell.executeSingleCommand("Get-Process");
            logger.println("Get-Process Result:" + response.getCommandOutput());
        }

        return "something";
    }
}
