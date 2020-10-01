package io.jenkins.plugins.SignPath.TestUtils;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

public class Some {
    private static final Random RANDOM = new Random();

    public static Integer integer() {
        return RANDOM.nextInt();
    }

    public static Integer integer(int minValue, int maxValue) {
        return minValue + RANDOM.nextInt(maxValue - minValue);
    }

    public static String stringNonEmpty(){
        return RandomStringUtils.random(1 + RANDOM.nextInt(100), true, true);
    }

    public static String sha1Hash(){
        String message = stringNonEmpty();

        MessageDigest digest = GetSha1Digest();
        assert digest != null;
        digest.reset();
        digest.update(message.getBytes(StandardCharsets.UTF_8));

        return String.format("%040x", new BigInteger(1, digest.digest()));
    }

    private static MessageDigest GetSha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            Assert.fail();
            return null;
        }
    }

    public static String url(){
        String fragmentEnd = RANDOM.nextBoolean() ? "/" : "";
        return "https://"+stringNonEmpty()+"/"+stringNonEmpty()+fragmentEnd;
    }

    public static String urlFragment() {
        String fragmentStart = RANDOM.nextBoolean() ? "/" : "";
        String fragmentEnd = RANDOM.nextBoolean() ? "/" : "";
        return fragmentStart + stringNonEmpty() + "/" + stringNonEmpty() + fragmentEnd;
    }

    public static byte[] bytes() {
        byte[] randomBytes = new byte[1+RANDOM.nextInt(100)];
        RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    public static UUID uuid() {
        return UUID.randomUUID();
    }
}
