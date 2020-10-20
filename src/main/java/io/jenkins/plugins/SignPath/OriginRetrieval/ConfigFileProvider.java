package io.jenkins.plugins.SignPath.OriginRetrieval;

import java.io.File;

/**
 * A wrapper to retrieve the jenkins project / run config file
 */
public interface ConfigFileProvider {
    /**
     * @return the current run's project config file
     */
    File retrieveBuildConfigFile();
}

