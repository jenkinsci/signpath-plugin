package io.jenkins.plugins.SignPath;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import hudson.security.ACL;
import jenkins.model.ArtifactManager;
import jenkins.model.Jenkins;
import jenkins.util.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SignStepExecution extends SynchronousStepExecution {

    private SignStep signStep;

    protected SignStepExecution(SignStep signStep, StepContext context) {
        super(context);
        this.signStep = signStep;
    }

    @Override
    protected String run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        Run run = getContext().get(Run.class);
        PrintStream logger = listener.getLogger();

        logger.println("signArtifact organizationId:" + signStep.getOrganizationId() + " waitForCompletion: "+ signStep.getWaitForCompletion());

        List<StringCredentials> credentials =
                CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstanceOrNull(), ACL.SYSTEM, Collections.emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId("TrustedBuildSystemToken");
        StringCredentials credential = CredentialsMatchers.firstOrNull(credentials, matcher);
        logger.println("TrustedBuildSystemToken=" + credential.getSecret().getPlainText());

        BuildData buildData = run.getAction(BuildData.class);
        String remoteUrls = buildData.getRemoteUrls().stream().collect(Collectors.joining());
        String branches = buildData.getBuildsByBranchName().keySet().stream().collect(Collectors.joining());
        String sha1Hashes = buildData.getBuildsByBranchName().entrySet().stream().map(stringBuildEntry -> stringBuildEntry.getValue().getSHA1().toString()).collect(Collectors.joining());
        logger.println("remote urls:" + remoteUrls + " branch:"+branches+" sha1: "+sha1Hashes);

        ArtifactManager artifactManager = run.getArtifactManager();
        VirtualFile unsignedArtifact = artifactManager.root().child("Calculator\\bin\\Release\\netcoreapp3.1\\publish\\Calculator.deps.json");
        if(!unsignedArtifact.exists()) {
            throw new IllegalArgumentException("artifact file does not exist");
        }

        try(InputStream s = unsignedArtifact.open();){
            String content = IOUtils.toString(s, StandardCharsets.UTF_8);
            logger.println(content);
        }

        return "something";
    }
}
