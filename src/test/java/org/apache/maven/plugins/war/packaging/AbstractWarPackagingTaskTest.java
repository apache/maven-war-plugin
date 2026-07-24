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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.war.util.WebappStructure;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.FilterWrapper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
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
        assertTrue(source.canExecute());

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "WEB-INF/lib/test.jar");

        AbstractWarPackagingTask task = createTask();
        task.copyFile(createContext(webappDir), source, destination, "WEB-INF/lib/test.jar", false);

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
        assertFalse(source.canExecute());

        File webappDir = new File(tempDir, "webapp");
        File destination = new File(webappDir, "WEB-INF/lib/test.jar");

        AbstractWarPackagingTask task = createTask();
        task.copyFile(createContext(webappDir), source, destination, "WEB-INF/lib/test.jar", false);

        assertTrue(destination.exists());
        assertFalse(destination.canExecute(), "copied file should not be executable");
    }

    private static AbstractWarPackagingTask createTask() {
        return new AbstractWarPackagingTask() {
            @Override
            public void performPackaging(WarPackagingContext context) {}
        };
    }

    private static WarPackagingContext createContext(File webappDir) {
        return new WarPackagingContext() {
            @Override
            public MavenProject getProject() {
                return null;
            }

            @Override
            public File getWebappDirectory() {
                return webappDir;
            }

            @Override
            public File getWebappSourceDirectory() {
                return null;
            }

            @Override
            public String[] getWebappSourceIncludes() {
                return new String[0];
            }

            @Override
            public boolean isWebappSourceIncludeEmptyDirectories() {
                return false;
            }

            @Override
            public String[] getWebappSourceExcludes() {
                return new String[0];
            }

            @Override
            public File getClassesDirectory() {
                return null;
            }

            @Override
            public boolean archiveClasses() {
                return false;
            }

            @Override
            public Log getLog() {
                return new Log() {
                    public void debug(CharSequence content) {}

                    public void debug(Throwable content) {}

                    public void debug(CharSequence content, Throwable error) {}

                    public void info(CharSequence content) {}

                    public void info(Throwable content) {}

                    public void info(CharSequence content, Throwable error) {}

                    public void warn(CharSequence content) {}

                    public void warn(Throwable content) {}

                    public void warn(CharSequence content, Throwable error) {}

                    public void error(CharSequence content) {}

                    public void error(Throwable content) {}

                    public void error(CharSequence content, Throwable error) {}

                    public boolean isDebugEnabled() {
                        return false;
                    }

                    public boolean isInfoEnabled() {
                        return false;
                    }

                    public boolean isWarnEnabled() {
                        return false;
                    }

                    public boolean isErrorEnabled() {
                        return false;
                    }
                };
            }

            @Override
            public File getOverlaysWorkDirectory() {
                return null;
            }

            @Override
            public ArchiverManager getArchiverManager() {
                return null;
            }

            @Override
            public MavenArchiveConfiguration getArchive() {
                return null;
            }

            @Override
            public JarArchiver getJarArchiver() {
                return null;
            }

            @Override
            public String getOutputFileNameMapping() {
                return null;
            }

            @Override
            public List<String> getFilters() {
                return null;
            }

            @Override
            public WebappStructure getWebappStructure() {
                return new WebappStructure(new ArrayList<>());
            }

            @Override
            public List<String> getOwnerIds() {
                return Collections.singletonList("test");
            }

            @Override
            public MavenFileFilter getMavenFileFilter() {
                return null;
            }

            @Override
            public List<FilterWrapper> getFilterWrappers() {
                return null;
            }

            @Override
            public boolean isNonFilteredExtension(String fileName) {
                return false;
            }

            @Override
            public boolean isFilteringDeploymentDescriptors() {
                return false;
            }

            @Override
            public ArtifactHandlerManager getArtifactHandlerManager() {
                return null;
            }

            @Override
            public MavenSession getSession() {
                return null;
            }

            @Override
            public String getResourceEncoding() {
                return null;
            }

            @Override
            public String getPropertiesEncoding() {
                return null;
            }

            @Override
            public Boolean isFailOnMissingWebXml() {
                return null;
            }

            @Override
            public void addResource(String resource) {}

            @Override
            public void deleteOutdatedResources() {}

            @Override
            public String getOutputTimestamp() {
                return null;
            }

            @Override
            public List<String> getPackagingExcludes() {
                return Collections.emptyList();
            }

            @Override
            public List<String> getPackagingIncludes() {
                return Collections.singletonList("**/**");
            }
        };
    }
}
