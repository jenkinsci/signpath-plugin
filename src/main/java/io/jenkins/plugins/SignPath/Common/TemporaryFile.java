package io.jenkins.plugins.SignPath.Common;

import hudson.util.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TemporaryFile implements Closeable {
    private final File temporaryFile;

    public TemporaryFile() throws IOException {
        temporaryFile = File.createTempFile("SignPathTemp", null);

        // we indicate that the java vm should delete the file on-exit
        temporaryFile.deleteOnExit();
    }

    public File getFile(){
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
    }
}
