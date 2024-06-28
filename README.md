[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/signpath-plugin/main)](https://ci.jenkins.io/job/Plugins/job/signpath-plugin/job/main/)

# SignPath Plugin

## Introduction

The SignPath Plugin for Jenkins allows you to integrate code signing with [SignPath](https://about.signpath.io) in your Jenkins pipeline.

## How to use this plugin

### Prerequisites

The following plugins must be installed:

- Credentials Binding [com.cloudbees.plugins.credentials]
- Git [hudson.plugins.git.util.Build]
- Pipeline

Make sure that the correct **Jenkins URL** is set unter _Manage Jenkins / Configure system._

### Configuration

On SignPath.io:

1. Add a Trusted Build System on SignPath and copy the generated **Trusted Build System Token**
2. Link the Trusted Build System to all projects that are build with it
3. Add one or more CI users (e.g. one per team) and copy the generated **API Token**

On Jenkins:

1. Store the **Trusted Build System Token** in a System Credential (Under Manage Jenkins / Manage Credentials) with the id `SignPath.TrustedBuildSystemToken`
2. Store the API Token(s) in a Credential so that it is available to the build pipelines of the respective projects (default id `SignPath.ApiToken`)

_Note: Currently, the SignPath plugin requires you to use **git** as your source control system. The git repository origin information is extracted and included in the signing request._

### Usage

In your `Jenkinsfile`, make sure the artifacts to be signed are pushed to the master node by adding a stage e.g.

```
stage('Archive') {
  steps {
    archiveArtifacts artifacts: "build-output/**", fingerprint: true
  }
}
```

Include the `submitSigningRequest` and optionally, the `getSignedArtifact` steps in your build pipeline. The artifacts to be signed need to be uploaded to the Jenkins master by calling the `archiveArtifacts` step.

#### Example: Submit a synchronous signing request

```
stage('Sign with SignPath') {
  steps {
    submitSigningRequest(
      organizationId: "${ORGANIZATION_ID}",
      projectSlug: "${PROJECT_SLUG}",
      signingPolicySlug: "${SIGNING_POLICY_SLUG}",
      artifactConfigurationSlug: "${ARTIFACT_CONFIGURATION_SLUG}",
      inputArtifactPath: "build-output/my-artifact.exe",
      outputArtifactPath: "build-output/my-artifact.signed.exe",
      waitForCompletion: true
    )
  }
}
```

#### Example: Submit an asynchronous signing request

```
stage('Sign with SignPath') {
  steps {
    script {
      signingRequestId = submitSigningRequest(
        organizationId: "${ORGANIZATION_ID}",
        projectSlug: "${PROJECT_SLUG}",
        signingPolicySlug: "${SIGNING_POLICY_SLUG}",
        artifactConfigurationSlug: "${ARTIFACT_CONFIGURATION_SLUG}",
        inputArtifactPath: "build-output/my-artifact.exe",
        outputArtifactPath: "build-output/my-artifact.signed.exe",
        waitForCompletion: false
      )
    }
  }
}
stage('Download Signed Artifact') {
  input {
    id "WaitForSigningRequestCompleted"
    message "Has the signing request completed?"
  }
  steps{
    getSignedArtifact( 
      organizationId: "${ORGANIZATION_ID}",
      signingRequestId: "${signingRequestId}",
      outputArtifactPath: "build-output/my-artifact.exe"
    )
  }
}

```

#### Parameters

| Parameter                                             |      |
| ----------------------------------------------------- | ---- |
| `apiUrl`                                              | (optional) The API endpoint of SignPath. Defaults to `https://app.signpath.io/api`
| `apiTokenCredentialId`                                | The ID of the credential containing the **API Token**. Defaults to `SignPath.ApiToken`. Recommended in scope "Global".
| `trustedBuildSytemTokenCredentialId`                  | The ID of the credential containing the **Trusted Build System Token**. Needs to be in scope "System".
| `organizationId`, `projectSlug`, `signingPolicySlug`  | Specify which organization, project and signing policy to use for signing. See the [official documentation](https://about.signpath.io/documentation/build-system-integration)
| `artifactConfigurationSlug`                           | (optional). Specify which artifact configuration to use. See the [official documentation](https://about.signpath.io/documentation/build-system-integration)
| `inputArtifactPath`                                   | The relative path of the artifact to be signed
| `outputArtifactPath`                                  | The relative path where the signed artifact is stored after signing
| `artifactConfigurationSlug`                           | (optional) The artifact configuration slug to use for signing
| `waitForCompletion`                                   | Set to `true` for synchronous and `false` for asynchronous signing requests
| `serviceUnavailableTimeoutInSeconds`                  | (optional, defaults to 600) Total time in seconds that the cmdlet will wait for a single service call to succeed (across several retries).
| `uploadAndDownloadRequestTimeoutInSeconds`            | (optional, defaults to 300)  HTTP timeout used for upload and download HTTP requests.
| `waitForCompletionTimeoutInSeconds`                   | (optional, defaults to 600) Maximum time in seconds that the step will wait for the signing request to complete.

## Build

https://ci.jenkins.io/job/Plugins/job/signpath-plugin/

## Support

The plugin is compatible with Jenkins 2.359 or higher.

Please refer to the support available in your respective [SignPath edition](https://about.signpath.io/product/editions).

## License

Copyright by SignPath GmbH

The SignPath Jenkins Plugin is being developed by [SignPath](https://about.signpath.io) and licensed under the **GNU General Public License v3 (GPL-3)**
