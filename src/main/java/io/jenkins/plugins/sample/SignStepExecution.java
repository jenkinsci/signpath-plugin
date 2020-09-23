package io.jenkins.plugins.sample;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.sun.xml.internal.rngom.parse.host.Base;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        EnvVars vars = getContext().get(hudson.EnvVars.class);
        Launcher launcher = getContext().get(Launcher.class);
        Run run = getContext().get(Run.class);

        PrintStream logger = listener.getLogger();

        String hostname = resolveHostname();
        logger.println("Running Sign Step for jenkins server: " + hostname);
        logger.println("Parameters, organizationId:" + signStep.getOrganizationId() + " waitForCompletion: "+ signStep.getWaitForCompletion());

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

        return "something";
    }

    private String resolveHostname(){
        String hostname="unresolved host";
        try
        {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch (UnknownHostException ex)
        {
            hostname = ex.toString();
        }

        return hostname;
    }
}
