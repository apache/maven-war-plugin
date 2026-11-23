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
package org.apache.maven.plugins.war.packaging;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class AbstractWarPackagingTaskTest {

    @TempDir
    File tempDir;

    // Windows does not support POSIX executable permissions, so canExecute()
    // may return true regardless of setExecutable(). These tests verify
    // permission normalization which is only meaningful on POSIX systems.
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    @Test
    void testCopyFileRemovesExecutablePermissions() throws Exception {
        assumeFalse(isWindows());
        File source = new File(tempDir, "source.jar");
        assertTrue(source.createNewFile());
        source.setExecutable(true, false);
        source.setReadable(true, false);
        source.setWritable(true, true);

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "WEB-INF/lib/test.jar");

        AbstractWarPackagingTask task = createTask();
        task.copyFile(new TestWarPackagingContext(webappDir), source, destination, "WEB-INF/lib/test.jar", false);

        assertTrue(destination.exists());
        assertFalse(destination.canExecute(), "copied file should not be executable");
        assertTrue(destination.canRead(), "copied file should be readable");
        assertTrue(destination.canWrite(), "copied file should be writable");
    }

    @Test
    void testCopyFilePreservesNonExecutable() throws Exception {
        assumeFalse(isWindows());
        File source = new File(tempDir, "source.jar");
        assertTrue(source.createNewFile());
        source.setExecutable(false, false);
        source.setReadable(true, false);
        source.setWritable(true, true);

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "WEB-INF/lib/test.jar");

        AbstractWarPackagingTask task = createTask();
        task.copyFile(new TestWarPackagingContext(webappDir), source, destination, "WEB-INF/lib/test.jar", false);

        assertTrue(destination.exists());
        assertFalse(destination.canExecute(), "copied file should not be executable");
        assertTrue(destination.canRead(), "copied file should be readable");
        assertTrue(destination.canWrite(), "copied file should be writable");
    }

    @Test
    void testCopyFileCopiesWhenContentSizeDiffersDespiteEqualTimestamp() throws Exception {
        // Reproduces GitHub #684: on filesystems with coarse mtime granularity (e.g. 1s on HFS+),
        // a source file modified within the same tick as the destination's last copy compares as
        // "not newer", so onlyIfModified must also fall back to a size check.
        File source = new File(tempDir, "source.txt");
        Files.write(source.toPath(), "changed content".getBytes(StandardCharsets.UTF_8));

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "test.txt");
        assertTrue(destination.getParentFile().mkdirs());
        Files.write(destination.toPath(), "old".getBytes(StandardCharsets.UTF_8));

        long sameModifiedTime = (System.currentTimeMillis() / 1000) * 1000;
        assertTrue(source.setLastModified(sameModifiedTime));
        assertTrue(destination.setLastModified(sameModifiedTime));

        AbstractWarPackagingTask task = createTask();
        boolean copied = task.copyFile(new TestWarPackagingContext(webappDir), source, destination, "test.txt", true);

        assertTrue(copied, "file should be copied when its size differs, even with an equal timestamp");
        assertEquals("changed content", new String(Files.readAllBytes(destination.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void testCopyFileSkipsDirectorySourceWhenNotModified() throws Exception {
        // A directory source is archived into a jar, so its own filesystem size is unrelated to the
        // destination jar's size; comparing them must not defeat the onlyIfModified mtime check here.
        File source = new File(tempDir, "source-dir");
        assertTrue(source.mkdirs());
        Files.write(new File(source, "entry.txt").toPath(), "content".getBytes(StandardCharsets.UTF_8));

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "test.jar");
        assertTrue(destination.getParentFile().mkdirs());
        Files.write(
                destination.toPath(),
                "previously archived jar bytes, a different size than the source directory's own entry"
                        .getBytes(StandardCharsets.UTF_8));

        long now = System.currentTimeMillis();
        assertTrue(source.setLastModified(now));
        assertTrue(destination.setLastModified(now + 1000));

        AbstractWarPackagingTask task = createTask();
        boolean copied = task.copyFile(new TestWarPackagingContext(webappDir), source, destination, "test.jar", true);

        assertFalse(
                copied,
                "a directory source considered up to date by mtime must not be re-archived just because "
                        + "its own filesystem size differs from the destination jar's size");
    }

    private static AbstractWarPackagingTask createTask() {
        return new AbstractWarPackagingTask() {
            @Override
            public void performPackaging(WarPackagingContext context) {}
        };
    }
}
