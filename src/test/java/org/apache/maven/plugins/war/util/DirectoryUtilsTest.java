/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.war.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void createsNonExistingDirectory() throws MojoExecutionException {
        Path dir = tempDir.resolve("newDir");
        assertFalse(Files.exists(dir));

        DirectoryUtils.createDirectoryIfNotExists(dir);

        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void createsNestedDirectoriesIncludingMissingParents() throws MojoExecutionException {
        Path dir = tempDir.resolve("a").resolve("b").resolve("c");
        assertFalse(Files.exists(dir.getParent()));

        DirectoryUtils.createDirectoryIfNotExists(dir);

        assertTrue(Files.isDirectory(tempDir.resolve("a")));
        assertTrue(Files.isDirectory(tempDir.resolve("a").resolve("b")));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void existingDirectoryIsLeftUntouched() throws IOException, MojoExecutionException {
        Path dir = Files.createDirectory(tempDir.resolve("existing"));
        Path content = Files.write(dir.resolve("file.txt"), "content".getBytes());

        DirectoryUtils.createDirectoryIfNotExists(dir);

        assertTrue(Files.isDirectory(dir));
        assertTrue(Files.exists(content), "existing content must not be removed");
        assertEquals("content", new String(Files.readAllBytes(content)));
    }

    @Test
    void callingTwiceDoesNotFail() throws MojoExecutionException {
        Path dir = tempDir.resolve("twice");

        DirectoryUtils.createDirectoryIfNotExists(dir);
        assertDoesNotThrow(() -> DirectoryUtils.createDirectoryIfNotExists(dir));

        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void throwsMojoExecutionExceptionIfFileExistsAtPath() throws IOException {
        Path file = Files.createFile(tempDir.resolve("someFile"));

        MojoExecutionException exception =
                assertThrows(MojoExecutionException.class, () -> DirectoryUtils.createDirectoryIfNotExists(file));

        assertEquals(
                "Could not create directory " + file + " because a file with the same name already exists",
                exception.getMessage());
        assertTrue(Files.isRegularFile(file), "existing file must not be removed");
        assertFalse(Files.isDirectory(file));
    }

    @Test
    void throwsMojoExecutionExceptionIfDirectoryCannotBeCreated() throws IOException {
        // a regular file as parent makes creating a subdirectory impossible
        Path parentFile = Files.createFile(tempDir.resolve("parentFile"));
        Path dir = parentFile.resolve("child");

        MojoExecutionException exception =
                assertThrows(MojoExecutionException.class, () -> DirectoryUtils.createDirectoryIfNotExists(dir));

        assertEquals("Could not create directory " + dir, exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
        assertFalse(Files.exists(dir));
    }
}
