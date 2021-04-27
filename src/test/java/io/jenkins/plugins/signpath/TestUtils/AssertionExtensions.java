package io.jenkins.plugins.signpath.TestUtils;

import static org.junit.Assert.assertTrue;

public class AssertionExtensions {
    public static void assertContains(String expected, String actual) {
        assertTrue(actual.contains(expected));
    }
}