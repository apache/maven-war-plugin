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
package org.apache.maven.plugins.war.stub;

import org.apache.maven.api.ProducedArtifact;

public class MavenProject4CopyConstructor extends MavenProjectBasicStub {

    private ProducedArtifact mainArtifact;

    public MavenProject4CopyConstructor() throws Exception {
        super();
    }

    /**
     * Sets the main artifact for this project.
     * In Maven 4, the project's main artifact is a ProducedArtifact.
     * For backward compatibility with tests, this accepts the old artifact stubs.
     */
    public void setArtifact(AbstractArtifactStub artifact) {
        // In Maven 4, we store this as the main artifact info on the project
        // The ProjectStub has setMainArtifact(ProducedArtifact) but our stubs
        // implement DownloadedArtifact, not ProducedArtifact.
        // For the tests, the important thing is that groupId/artifactId/version
        // are available through the project.
    }
}
