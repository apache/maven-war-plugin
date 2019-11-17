package org.apache.maven.plugins.war.packaging;

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

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.war.util.WebappStructure;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.utils.io.FileUtils.FilterWrapper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * The packaging context.
 *
 * @author Stephane Nicoll
 */
public interface WarPackagingContext
{
    /**
     * Returns the maven project.
     *
     * @return the project
     */
    MavenProject getProject();

    /**
     * Returns the webapp directory. Packaging tasks should use this directory to generate the webapp.
     *
     * @return the webapp directory
     */
    File getWebappDirectory();

    /**
     * Returns the main webapp source directory.
     *
     * @return the webapp source directory
     */
    File getWebappSourceDirectory();

    /**
     * Returns the webapp source includes.
     *
     * @return the webapp source includes
     */
    String[] getWebappSourceIncludes();

    /**
     * Returns {@code true} if empty directories should be includes, otherwise {@code false}
     *
     * @return {@code true} if empty directories should be includes, otherwise {@code false}
     */
    boolean isWebappSourceIncludeEmptyDirectories();

    /**
     * Returns the webapp source excludes.
     *
     * @return the webapp source excludes
     */
    String[] getWebappSourceExcludes();

    /**
     * Returns the directory holding generated classes.
     *
     * @return the classes directory
     */
    File getClassesDirectory();

    /**
     * Specify whether the classes resources should be archived in the <tt>WEB-INF/lib</tt> of the generated web app.
     *
     * @return true if the classes should be archived, false otherwise
     */
    boolean archiveClasses();

    /**
     * Returns the logger to use to output logging event.
     *
     * @return the logger
     */
    Log getLog();

    /**
     * Returns the directory to unpack dependent WARs into if needed.
     *
     * @return the overlays work directory
     */
    File getOverlaysWorkDirectory();

    /**
     * Returns the archiver manager to use.
     *
     * @return the archiver manager
     */
    ArchiverManager getArchiverManager();

    /**
     * The maven archive configuration to use.
     *
     * @return the maven archive configuration
     */
    MavenArchiveConfiguration getArchive();

    /**
     * Returns the Jar archiver needed for archiving classes directory into jar file under WEB-INF/lib.
     *
     * @return the jar archiver to user
     */
    JarArchiver getJarArchiver();

    /**
     * Returns the output file name mapping to use, if any. Returns <tt>null</tt> if no file name mapping is set.
     *
     * @return the output file name mapping or <tt>null</tt>
     */
    String getOutputFileNameMapping();

    /**
     * Returns the list of filter files to use.
     *
     * @return a list of filter files
     */
    List<String> getFilters();

    /**
     * Returns the {@link WebappStructure}.
     *
     * @return the webapp structure
     */
    WebappStructure getWebappStructure();

    /**
     * Returns the list of registered overlays for this session.
     *
     * @return the list of registered overlays, including the current project
     */
    List<String> getOwnerIds();

    /**
     * Returns the {@link MavenFileFilter} instance to use.
     *
     * @return the maven file filter to use
     * @since 2.1-alpha-2
     */
    MavenFileFilter getMavenFileFilter();

    /**
     * @return {@link List} of {@link FilterWrapper}
     * @since 2.1-alpha-2
     */
    List<FilterWrapper> getFilterWrappers();

    /**
     * Specify if the given <tt>fileName</tt> belongs to the list of extensions that must not be filtered
     *
     * @param fileName the name of file
     * @return <tt>true</tt> if it should not be filtered, <tt>false</tt> otherwise
     * @since 2.1-alpha-2
     */
    boolean isNonFilteredExtension( String fileName );

    /**
     * @return filtering deployment descriptor.
     */
    boolean isFilteringDeploymentDescriptors();

    /**
     * @return {@link ArtifactFactory}
     */
    ArtifactFactory getArtifactFactory();

    /**
     * Returns the Maven session.
     *
     * @return the Maven session
     * @since 2.2
     */
    MavenSession getSession();

    /**
     * Returns the encoding to use for resources.
     *
     * @return the resource encoding
     * @since 2.3
     */
    String getResourceEncoding();

    /**
     * @return to use jvmChmod rather than forking chmod cli
     * @since 2.4
     */
    boolean isUseJvmChmod();

    /**
     * Returns the flag that switch on/off the missing web.xml validation
     *
     * @return failOnMissingWebXml
     */
    Boolean isFailOnMissingWebXml();

    /**
     * Used to keep track of existing resources and all copied files.
     * All others are outdated and should be removed.
     * This prevent calling <code>clean</code> when resources are removed. 
     * 
     * @return the outdated resources
     * @since 3.2.4
     */
    Collection<String> getOutdatedResources();
}
