package io.jenkins.plugins.signpath.Common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryFileTest {

    @Test
    void create_withName() throws IOException {
        // ACT
        TemporaryFile temporaryFile = new TemporaryFile("file.txt");

        // ASSERT
        assertTrue(temporaryFile.getAbsolutePath().endsWith("file.txt"));
    }

    @Test
    void create_withSubfolder() throws IOException {
        // ACT
        TemporaryFile temporaryFile = new TemporaryFile("subfolder/file.txt");

        // ASSERT
        assertFalse(temporaryFile.getAbsolutePath().contains("subfolder"));
        assertTrue(temporaryFile.getAbsolutePath().endsWith("file.txt"));
    }

    @Test
    void navigateToParent_throws() {
        // ACT
        Executable act = () -> new TemporaryFile("../file.txt");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals("Navigating to parent is not allowed.", ex.getMessage());
    }
}
