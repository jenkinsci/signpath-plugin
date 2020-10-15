package io.jenkins.plugins.SignPath.Common;


import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;

import static org.junit.Assert.*;

public class TemporaryFileTest {

    @Test
    public void create_withName() throws IOException {
        // ACT
        TemporaryFile temporaryFile = new TemporaryFile("file.txt");

        // ASSERT
        assertTrue(temporaryFile.getAbsolutePath().endsWith("file.txt"));
        temporaryFile.close();
    }

    @Test
    public void navigateToParent_throws() {
        // ACT
        ThrowingRunnable act = () -> new TemporaryFile("../file.txt");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals(ex.getMessage(), String.format("Navigating to parent is not allowed."));
    }
}
