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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Locale;

import org.apache.maven.plugins.war.util.WebappStructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void testCopyFileRemovesExecutablePermissions() throws IOException {
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
    void testCopyFilePreservesNonExecutable() throws IOException {
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
    void incrementalCopyReplacesFileSupersededFromKnownOwner() throws IOException {
        assertSupersededFileIsReplaced(false);
    }

    @Test
    void incrementalCopyReplacesFileSupersededFromUnknownOwner() throws IOException {
        assertSupersededFileIsReplaced(true);
    }

    private void assertSupersededFileIsReplaced(boolean unknownOwner) throws IOException {
        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "WEB-INF/web.xml");
        assertTrue(destination.getParentFile().mkdirs());
        Files.write(destination.toPath(), "old owner".getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(destination.toPath(), FileTime.fromMillis(2_000));

        File source = new File(tempDir, unknownOwner ? "unknown-owner.xml" : "known-owner.xml");
        Files.write(source.toPath(), "new owner".getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(source.toPath(), FileTime.fromMillis(1_000));

        WebappStructure structure = new WebappStructure(Collections.emptyList()) {
            @Override
            public void registerFile(String id, String path, RegistrationCallback callback) throws IOException {
                if (unknownOwner) {
                    callback.supersededUnknownOwner(id, path, "removed-overlay");
                } else {
                    callback.superseded(id, path, "current-overlay");
                }
            }
        };
        TestWarPackagingContext context = new TestWarPackagingContext(webappDir) {
            @Override
            public WebappStructure getWebappStructure() {
                return structure;
            }

            @Override
            public boolean isIncremental() {
                return true;
            }
        };

        createTask().copyFile("new-overlay", context, source, "WEB-INF/web.xml");

        assertTrue(
                java.util.Arrays.equals(
                        "new owner".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(destination.toPath())),
                "a superseding overlay must replace the previous owner's file regardless of timestamps");
    }

    private static AbstractWarPackagingTask createTask() {
        return new AbstractWarPackagingTask() {
            @Override
            public void performPackaging(WarPackagingContext context) {}
        };
    }
}
