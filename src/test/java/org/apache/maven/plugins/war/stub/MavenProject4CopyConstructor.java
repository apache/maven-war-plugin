package org.apache.maven.plugins.war.stub;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;

public class MavenProject4CopyConstructor
    extends MavenProjectBasicStub
{
    protected ModelStub model;

    public MavenProject4CopyConstructor()
        throws Exception
    {
        initializeParentFields();
    }

    public List<Artifact> getAttachedArtifacts()
    {
        return new LinkedList<>();
    }

    // to prevent the MavenProject copy constructor from blowing up
    private void initializeParentFields()
    {
        // the pom should be located in the isolated dummy root         
        super.setFile( new File( getBasedir(), "pom.xml" ) );
        super.setDependencyArtifacts( new HashSet<Artifact>() );
        super.setArtifacts( new HashSet<Artifact>() );
        super.setExtensionArtifacts( new HashSet<Artifact>() );
        super.setRemoteArtifactRepositories( new LinkedList<ArtifactRepository>() );
        super.setPluginArtifactRepositories( new LinkedList<ArtifactRepository>() );
        super.setCollectedProjects( new LinkedList<MavenProject>() );
        super.setActiveProfiles( new LinkedList<Profile>() );
        super.setOriginalModel( null );
        super.setExecutionProject( this );
        super.setBuild( getBuild() );
    }
}
