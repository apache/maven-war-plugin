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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarUtilsTest {

    private static Artifact createArtifact(
            String groupId, String artifactId, String version, String type, String classifier, String scope) {
        return new ArtifactStub() {
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public String getClassifier() {
                return classifier;
            }

            @Override
            public String getScope() {
                return scope;
            }
        };
    }

    private static Dependency createDependency(
            String groupId, String artifactId, String version, String type, String classifier, String scope) {
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        dep.setType(type);
        dep.setClassifier(classifier);
        dep.setScope(scope);
        return dep;
    }

    @Test
    void isRelatedShouldReturnTrueWhenAllAttributesMatch() {
        Artifact artifact = createArtifact("g", "a", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g", "a", "1.0", "jar", null, "compile");
        assertTrue(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenGroupIdDiffers() {
        Artifact artifact = createArtifact("g1", "a", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g2", "a", "1.0", "jar", null, "compile");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenArtifactIdDiffers() {
        Artifact artifact = createArtifact("g", "a1", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g", "a2", "1.0", "jar", null, "compile");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenVersionDiffers() {
        Artifact artifact = createArtifact("g", "a", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g", "a", "2.0", "jar", null, "compile");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenTypeDiffers() {
        Artifact artifact = createArtifact("g", "a", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g", "a", "1.0", "war", null, "compile");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenClassifierDiffers() {
        Artifact artifact = createArtifact("g", "a", "1.0", "jar", "client", "compile");
        Dependency dependency = createDependency("g", "a", "1.0", "jar", "server", "compile");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }

    @Test
    void isRelatedShouldReturnFalseWhenScopeDiffers() {
        Artifact artifact = createArtifact("g", "a", "1.0", "jar", null, "compile");
        Dependency dependency = createDependency("g", "a", "1.0", "jar", null, "provided");
        assertFalse(WarUtils.isRelated(artifact, dependency));
    }
}
