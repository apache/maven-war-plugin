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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractWarPackagingTaskTest {

    @TempDir
    File tempDir;

    @Test
    void testCopyFileRemovesExecutablePermissions() throws IOException {
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
    }

    @Test
    void testCopyFileKeepsNonExecutable() throws IOException {
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
    }

    private static AbstractWarPackagingTask createTask() {
        return new AbstractWarPackagingTask() {
            @Override
            public void performPackaging(WarPackagingContext context) {}
        };
    }
}
