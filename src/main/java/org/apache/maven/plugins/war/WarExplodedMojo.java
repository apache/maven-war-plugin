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

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Create an exploded webapp in a specified directory.
 */
@Mojo(
        name = "exploded",
        defaultPhase = LifecyclePhase.PACKAGE,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class WarExplodedMojo extends AbstractWarMojo {
    @Inject
    public WarExplodedMojo(
            ArtifactHandlerManager artifactHandlerManager,
            ArchiverManager archiverManager,
            MavenFileFilter mavenFileFilter,
            MavenResourcesFiltering mavenResourcesFiltering) {
        super(artifactHandlerManager, archiverManager, mavenFileFilter, mavenResourcesFiltering);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Exploding webapp");

        buildExplodedWebapp(getWebappDirectory());
    }
}
