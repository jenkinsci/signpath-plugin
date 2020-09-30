package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;

import java.io.File;

public class DefaultConfigFileProvider implements IConfigFileProvider {

    @Override
    public File retrieveBuildConfigFile(Run run) {
        return run.getParent().getConfigFile().getFile();
    }
}
