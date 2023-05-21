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

import java.util.ArrayList;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 * @author Stephane Nicoll
 */
public class WebappStructureTest extends TestCase {
    public void testUnknownFileNotAvailable() {
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        assertFalse(structure.isRegistered("/foo/bar.txt"));
    }

    public void testRegisterSamePathTwice() {
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        structure.registerFile("overlay1", "WEB-INF/web.xml");
        assertFalse(structure.registerFile("currentBuild", "WEB-INF/web.xml"));
    }

    public void testRegisterForced() {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        assertFalse("New file should return false", structure.registerFileForced("overlay1", path));
        assertEquals("overlay1", structure.getOwner(path));
    }

    public void testRegisterSamePathTwiceForced() {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        structure.registerFile("overlay1", path);
        assertEquals("overlay1", structure.getOwner(path));
        assertTrue("owner replacement should have returned true", structure.registerFileForced("currentBuild", path));
        assertEquals("currentBuild", structure.getOwner(path));
    }

    protected Dependency createDependency(
            String groupId, String artifactId, String version, String type, String scope, String classifier) {
        final Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        if (type == null) {
            dep.setType("jar");
        } else {
            dep.setType(type);
        }
        if (scope != null) {
            dep.setScope(scope);
        } else {
            dep.setScope(Artifact.SCOPE_COMPILE);
        }
        if (classifier != null) {
            dep.setClassifier(classifier);
        }
        return dep;
    }

    protected Dependency createDependency(
            String groupId, String artifactId, String version, String type, String scope) {
        return createDependency(groupId, artifactId, version, type, scope, null);
    }

    protected Dependency createDependency(String groupId, String artifactId, String version, String type) {
        return createDependency(groupId, artifactId, version, type, null);
    }

    protected Dependency createDependency(String groupId, String artifactId, String version) {
        return createDependency(groupId, artifactId, version, null);
    }
}
