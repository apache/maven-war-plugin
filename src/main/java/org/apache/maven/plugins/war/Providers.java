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
package org.apache.maven.plugins.war;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.services.ProjectManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.JarUnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.DefaultArchiverManager;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.war.WarUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * DI provider bindings for services not directly available in the Maven 4 plugin DI container.
 */
@Named
class Providers {

    @Provides
    static ProjectManager projectManager(Session session) {
        return session.getService(ProjectManager.class);
    }

    @Provides
    static BuildContext buildContext() {
        return new SimpleBuildContext();
    }

    @Provides
    static ArchiverManager archiverManager() {
        Map<String, javax.inject.Provider<Archiver>> archivers = new HashMap<>();
        archivers.put("jar", JarArchiver::new);
        archivers.put("war", WarArchiver::new);
        archivers.put("zip", ZipArchiver::new);

        Map<String, javax.inject.Provider<UnArchiver>> unarchivers = new HashMap<>();
        unarchivers.put("jar", JarUnArchiver::new);
        unarchivers.put("war", WarUnArchiver::new);
        unarchivers.put("zip", ZipUnArchiver::new);

        return new DefaultArchiverManager(archivers, unarchivers, new HashMap<>());
    }

    /**
     * A simple {@link BuildContext} implementation that does not depend on
     * {@code plexus-container-default} (which is unavailable in Maven 4).
     */
    static class SimpleBuildContext implements BuildContext {

        @Override
        public boolean hasDelta(String relpath) {
            return true;
        }

        @Override
        public boolean hasDelta(File file) {
            return true;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean hasDelta(List relpaths) {
            return true;
        }

        @Override
        public void refresh(File file) {
            // no-op
        }

        @Override
        public OutputStream newFileOutputStream(File file) throws IOException {
            return Files.newOutputStream(file.toPath());
        }

        @Override
        public Scanner newScanner(File basedir) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(basedir);
            return scanner;
        }

        @Override
        public Scanner newDeleteScanner(File basedir) {
            return newScanner(basedir);
        }

        @Override
        public Scanner newScanner(File basedir, boolean ignoreDelta) {
            return newScanner(basedir);
        }

        @Override
        public boolean isIncremental() {
            return false;
        }

        @Override
        public void setValue(String key, Object value) {
            // no-op
        }

        @Override
        public Object getValue(String key) {
            return null;
        }

        @Override
        public void addWarning(File file, int line, int column, String message, Throwable cause) {
            // no-op
        }

        @Override
        public void addError(File file, int line, int column, String message, Throwable cause) {
            // no-op
        }

        @Override
        public void addMessage(File file, int line, int column, String message, int severity, Throwable cause) {
            // no-op
        }

        @Override
        public void removeMessages(File file) {
            // no-op
        }

        @Override
        public boolean isUptodate(File output, File input) {
            return output != null
                    && output.exists()
                    && input != null
                    && input.exists()
                    && output.lastModified() > input.lastModified();
        }
    }
}
