package io.jenkins.plugins.signpath.TestUtils;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

public class WorkflowJobUtil {

    public static WorkflowJob createWorkflow(JenkinsRule jenkins, String name, String script) throws IOException {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, name);
        job.setDefinition(new CpsFlowDefinition(String.format("node {%s}", script), true));
        return job;
    }

    public static WorkflowJob createWorkflowJob(JenkinsRule jenkins,
                                                 String apiUrl,
                                                 String trustedBuildSystemTokenCredentialId,
                                                 String apiTokenCredentialId,
                                                 String organizationId,
                                                 String signingRequestId) throws IOException {
        return createWorkflow(jenkins, "SignPath",
                "getSignedArtifact(apiUrl: '" + apiUrl + "', " +
                        "outputArtifactPath: 'signed.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "signingRequestId: '" + signingRequestId + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10);");
    }

    public static WorkflowJob createWorkflowJobWithOptionalParameters(JenkinsRule jenkins,
                                                                       String apiUrl,
                                                                       String trustedBuildSystemTokenCredentialId,
                                                                       String apiTokenCredentialId,
                                                                       String organizationId,
                                                                       String projectSlug,
                                                                       String signingPolicySlug,
                                                                       String unsignedArtifactString,
                                                                       String artifactConfigurationSlug,
                                                                       String description,
                                                                       String userDefinedParamName,
                                                                       String userDefinedParamValue,
                                                                       boolean waitForCompletion) throws IOException {
        return WorkflowJobUtil.createWorkflow(jenkins,
                "SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest( apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        "outputArtifactPath: 'signed.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "artifactConfigurationSlug: '" + artifactConfigurationSlug + "'," +
                        "description: '" + description + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "parameters: [ " + userDefinedParamName + ": \"" + userDefinedParamValue + "\" ]," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");
    }

    public static WorkflowJob createWorkflowJob(JenkinsRule jenkins,
                                                 String apiUrl,
                                                 String trustedBuildSystemTokenCredentialId,
                                                 String apiTokenCredentialId,
                                                 String organizationId,
                                                 String projectSlug,
                                                 String signingPolicySlug,
                                                 String unsignedArtifactString,
                                                 boolean waitForCompletion) throws IOException {
        String outputArtifactPath = waitForCompletion
                ? "outputArtifactPath: 'signed.exe', "
                : "";

        return createWorkflow(jenkins,
                "SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        outputArtifactPath +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");
    }


}