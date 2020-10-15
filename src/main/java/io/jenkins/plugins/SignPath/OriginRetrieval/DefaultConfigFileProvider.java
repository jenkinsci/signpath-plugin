package io.jenkins.plugins.SignPath.OriginRetrieval;

import hudson.model.Run;

import java.io.File;

/**
 * Default implementation of the
 *
 * @see ConfigFileProvider interface
 */
public class DefaultConfigFileProvider implements ConfigFileProvider {

    private final Run<?, ?> run;

    public DefaultConfigFileProvider(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public File retrieveBuildConfigFile() {
        return run.getParent().getConfigFile().getFile();
    }
}
