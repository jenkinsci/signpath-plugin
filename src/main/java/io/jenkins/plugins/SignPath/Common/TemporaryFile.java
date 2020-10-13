package io.jenkins.plugins.SignPath.Common;

import hudson.util.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * A helper class for handling (creating and disposing) temporary files
 */
public class TemporaryFile implements Closeable {
    private final File temporaryDirectory;
    private final File temporaryFile;

    public TemporaryFile() throws IOException {
        temporaryDirectory = null;
        temporaryFile = File.createTempFile("SignPathJenkinsPluginTemp", null);

        temporaryFile.deleteOnExit();
    }

    public TemporaryFile(String name) throws IOException {
        // TODO SIGN-3415: Check if name navigates to parent ../ ?
        // in order to create a file with a specific name, we put it in a custom temporary directory
        temporaryDirectory = Files.createTempDirectory("SignPathJenkinsPluginTemp").toFile();
        temporaryFile = new File(temporaryDirectory, name);

        // this should never throw (only if file-already exists, but the temp directory should be unique)
        assert temporaryFile.createNewFile();

        // we indicate that the java vm should delete the file on-exit
        temporaryDirectory.deleteOnExit();
    }

    public File getFile() {
        return temporaryFile;
    }

    public String getAbsolutePath() {
        return temporaryFile.getAbsolutePath();
    }

    public void copyFrom(InputStream in) throws IOException {
        IOUtils.copy(in, temporaryFile);
    }

    @Override
    public void close() {
        //noinspection ResultOfMethodCallIgnored
        temporaryFile.delete();

        if (temporaryDirectory != null) {
            //noinspection ResultOfMethodCallIgnored
            temporaryDirectory.delete();
        }
    }
}
