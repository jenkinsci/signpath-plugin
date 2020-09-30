package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;

import java.io.File;

public interface IConfigFileProvider {
    File retrieveBuildConfigFile(Run run);
}

