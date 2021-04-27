package io.jenkins.plugins.signpath.Common;

import hudson.util.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        // in order to create a file with a specific name, we put it in a custom temporary directory
        temporaryDirectory = Files.createTempDirectory("SignPathJenkinsPluginTemp").toFile();
        File newTemporaryFile = new File(temporaryDirectory, name);

        if(!newTemporaryFile.getCanonicalPath().contains(temporaryDirectory.getCanonicalPath()))
            throw new IllegalAccessError("Navigating to parent is not allowed.");

        // we only allow 1 nesting level and strip the rest to avoid problems with long paths
        String fileName = Paths.get(newTemporaryFile.getCanonicalPath()).getFileName().toString();
        temporaryFile = new File(temporaryDirectory, fileName);


        // this should never throw (only if file-already exists, but the temp directory should be unique)
        assert temporaryFile.createNewFile();

        // we indicate that the java vm should delete the file on-exit
        temporaryFile.deleteOnExit();
        temporaryDirectory.deleteOnExit();

        // we also add a shutdown hook as the deleteOnExit does not work for directories (because it is not empty)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(temporaryDirectory)));
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
