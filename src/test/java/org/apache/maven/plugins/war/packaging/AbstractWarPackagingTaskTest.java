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
import java.util.Locale;

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

    private static AbstractWarPackagingTask createTask() {
        return new AbstractWarPackagingTask() {
            @Override
            public void performPackaging(WarPackagingContext context) {}
        };
    }
}
