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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Generate the webapp in the WAR source directory.
 */
@Mojo(name = "inplace", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class WarInPlaceMojo extends AbstractWarMojo {

    @Inject
    public WarInPlaceMojo(
            JarArchiver jarArchiver,
            ArtifactFactory artifactFactory,
            ArchiverManager archiverManager,
            @Named("default") MavenFileFilter mavenFileFilter,
            @Named("default") MavenResourcesFiltering mavenResourcesFiltering) {
        super(jarArchiver, artifactFactory, archiverManager, mavenFileFilter, mavenResourcesFiltering);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating webapp in source directory [" + getWarSourceDirectory() + "]");

        buildExplodedWebapp(getWarSourceDirectory());
    }
}
