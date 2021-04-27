# signpath.jenkinsplugin

## Introduction

The SignPath Jenkins Plugin provides two useful steps that can be used to sign your build artifacts with <a href="https://www.signpath.io">SignPath.io</a>.

## Getting started

Plugins required
- Credentials Binding [com.cloudbees.plugins.credentials]
- Git [hudson.plugins.git.util.Build]
- Pipeline

Add a new *System* Secret **SignPath.TrustedBuildSystemToken** to Jenkins.
This secret should contain the secret token that you can retrieve when you create a new trusted build system in your SignPath Organization.

Configure your **Jenkins Root Url**, this is required to generate the build url that is part of the Origin in the signing requests.

Make sure that your builds **use git** as your source control system. For the origin verification feature (always enabled) it is required that all builds signed by SignPath include the *Git Build Data*.

Install the latest SignPath PowerShell Module on your Jenkins master node from the gallery via **Install-Module SignPath**.
This is required to connect to the SignPath Api Endpoint at the moment.

## Contributing

Setup your machine
- Install Intellij Idea (recommended)
- Setup Build and run: **hpi:run** (Maven with the repo as working directory)
- Setup Package build: **package** (Maven with the repo as working directory)

Run the tests
- Install the correct version of the SignPath PowerShell Module (latest version from the Gallery)

Run the Plugin locally
- Set up you System as Jenkins Master (as described in the Getting started section).
- Set up a declarative Pipeline Project and configure it to use the github example: https://github.com/Inspyro/jenkinstest [Jenkinsfile or JenkinsfileAsync].
- Set up an agent - create a new agent and run it via JNLP (Java). Another option is to the provided docker agent (See dedicated section)