package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;

import java.io.File;

public class DefaultConfigFileProvider implements IConfigFileProvider {

    private final Run<?, ?> run;

    public DefaultConfigFileProvider(Run<?, ?> run){
        this.run = run;
    }

    @Override
    public File retrieveBuildConfigFile() {
        return run.getParent().getConfigFile().getFile();
    }
}
