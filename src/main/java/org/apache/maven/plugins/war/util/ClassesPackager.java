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

import java.io.File;
import java.io.IOException;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.packaging.AbstractWarPackagingTask;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

/**
 * Packages the content of the classes directory.
 *
 * @author Stephane Nicoll
 */
public class ClassesPackager {

    /**
     * Package the classes
     *
     * @param classesDirectory the classes directory
     * @param targetFile the target file
     * @param jarArchiver the jar archiver to use
     * @param session the current session
     * @param project the related project
     * @param archiveConfiguration the archive configuration to use
     * @param outputTimestamp the output timestamp for reproducibility
     * @throws MojoExecutionException if an error occurred while creating the archive
     */
    public void packageClasses(
            File classesDirectory,
            File targetFile,
            JarArchiver jarArchiver,
            MavenSession session,
            MavenProject project,
            MavenArchiveConfiguration archiveConfiguration,
            String outputTimestamp)
            throws MojoExecutionException {

        try {
            final MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);
            archiver.setOutputFile(targetFile);
            archiver.setCreatedBy("Maven WAR Plugin", "org.apache.maven.plugins", "maven-war-plugin");
            archiver.configureReproducibleBuild(outputTimestamp);
            archiver.getArchiver().addDirectory(classesDirectory);
            archiver.createArchive(session, project, archiveConfiguration);
        } catch (ArchiverException | ManifestException | IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Could not create classes archive", e);
        }
    }

    /**
     * Returns the classes directory from the specified webapp directory.
     *
     * @param webappDirectory the webapp directory
     * @return the classes directory of the specified webapp directory
     */
    public File getClassesDirectory(File webappDirectory) {
        return new File(webappDirectory, AbstractWarPackagingTask.CLASSES_PATH);
    }
}
