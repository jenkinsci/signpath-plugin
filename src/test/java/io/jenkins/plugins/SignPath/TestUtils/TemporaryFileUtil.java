package io.jenkins.plugins.SignPath.TestUtils;

import hudson.Util;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TemporaryFileUtil {
    public static TemporaryFile create(byte[] content) throws IOException {
        TemporaryFile temporaryFile = new TemporaryFile();

        try (InputStream in = new ByteArrayInputStream(content)) {
            temporaryFile.copyFrom(in);
        }

        return temporaryFile;
    }

    public static byte[] getContentAndDispose(TemporaryFile temporaryFile) throws IOException {
        try (TemporaryFile t = temporaryFile) {
            return Files.readAllBytes(Paths.get(temporaryFile.getAbsolutePath()));
        }
    }

    public static String getAbsolutePathAndDispose(TemporaryFile temporaryFile) {
        try (TemporaryFile t = temporaryFile) {
            return t.getAbsolutePath();
        }
    }

    public static String getDigestAndDispose(TemporaryFile temporaryFile) throws IOException, NoSuchAlgorithmException {
        try (TemporaryFile t = temporaryFile) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(temporaryFile.getFile())) {
                try (BufferedInputStream bis = new BufferedInputStream(fis)) {
                    try (DigestInputStream dis = new DigestInputStream(bis, md5)) {
                        while (dis.read() != -1) {
                        }
                        return Util.toHexString(md5.digest());
                    }
                }
            }
        }
    }
}