package io.jenkins.plugins.sample;

import hudson.EnvVars;
import hudson.model.TaskListener;
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

public class SignStepExecution extends SynchronousStepExecution {

    protected SignStepExecution(StepContext context) {
        super(context);
    }

    @Override
    protected String run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        PrintStream logger = listener.getLogger();

        String hostname = resolveHostname();
        logger.println("Running Sign Step for jenkins server: " + hostname);

        EnvVars vars = getContext().get(hudson.EnvVars.class);
        String jenkinsHome = vars.get("JENKINS_HOME");
        String jobName = vars.get("JOB_BASE_NAME");
        String buildId = vars.get("BUILD_ID");

        Path credentialsPath = Paths.get(jenkinsHome, "credentials.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(credentialsPath.toString());
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("//secret[../id/text()=\"TrustedBuildSystemToken\"]/text()");
        String credential = expr.evaluate(doc);
        String decryptedCredential = hudson.util.Secret.decrypt(credential).getPlainText();
        logger.println("TrustedBuildSystemToken=" + decryptedCredential);

        Path configPath = Paths.get(jenkinsHome, "jobs", jobName,"builds",buildId,"build.xml");
        logger.println("Reading config from: "+configPath.toString());

        String content = new String(Files.readAllBytes(configPath));
        logger.println(content);

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
