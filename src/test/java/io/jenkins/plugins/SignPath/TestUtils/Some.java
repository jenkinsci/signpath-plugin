package io.jenkins.plugins.SignPath.TestUtils;

import org.apache.commons.lang.RandomStringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Some {
    private static final Random RANDOM = new Random();

    public static Integer integer() {
        return RANDOM.nextInt();
    }

    public static String stringNonEmpty(){
        return RandomStringUtils.random(1 + RANDOM.nextInt(100), true, true);
    }

    public static String sha1Hash(){
        String message = stringNonEmpty();
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        digest.reset();

        try {
            digest.update(message.getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return String.format("%040x", new BigInteger(1, digest.digest()));
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
}
