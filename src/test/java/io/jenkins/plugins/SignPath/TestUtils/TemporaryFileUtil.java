package io.jenkins.plugins.SignPath.TestUtils;

import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TemporaryFileUtil {
    public static TemporaryFile create(byte[] content) throws IOException {
        TemporaryFile temporaryFile = new TemporaryFile();

        try (InputStream in = new ByteArrayInputStream(content)) {
            temporaryFile.copyFrom(in);
        }

        return temporaryFile;
    }

    public static byte[] getContent(TemporaryFile temporaryFile) throws IOException {
        return Files.readAllBytes(Paths.get(temporaryFile.getAbsolutePath()));
    }
}
