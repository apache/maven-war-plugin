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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.plugin.testing.stubs.ArtifactStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the mapping of file names.
 */
class FileNameMappingTest {

    private static final String DEFAULT_MAPPING = "@{artifactId}@-@{baseVersion}@@{dashClassifier?}@.@{extension}@";

    @Test
    void completeMapping() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals(
                "maven-test-lib-1.0.jar", FileNameMapping.evaluate("@{artifactId}@-@{version}@.@{extension}@", jar));
    }

    @Test
    void noVersionMapping() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals("maven-test-lib.jar", FileNameMapping.evaluate("@{artifactId}@.@{extension}@", jar));
    }

    @Test
    void mappingWithGroupId() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals(
                "org.apache.sample-maven-test-lib-1.0.jar",
                FileNameMapping.evaluate("@{groupId}@-@{artifactId}@-@{version}@.@{extension}@", jar));
    }

    @Test
    void mappingWithClassifier() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", "classifier");
        assertEquals("maven-test-lib-1.0-classifier.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    @Test
    void mappingWithNullClassifier() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    /**
     * Test for #65. When classifier is null, the default classifier mapping should
     * produce a clean filename without a trailing dash.
     */
    @Test
    void mappingWithNullClassifierShouldNotHaveTrailingDash() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    @Test
    void mappingWithEmptyClassifierShouldNotHaveTrailingDash() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", "");
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    /**
     * Test for MWAR-212.
     */
    @Test
    void mappingWithOptionalClassifier() throws Exception {
        final String mappingWithOptionalClassifier1 = "@{artifactId}@-@{version}@@{dashClassifier}@.@{extension}@";
        final String mappingWithOptionalClassifier2 = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", null);
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier1, jar));
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier2, jar));

        jar = artifact("org.apache.sample", "maven-test-lib", "1.0", "classifier");
        assertEquals(
                "maven-test-lib-1.0-classifier.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier1, jar));
        assertEquals(
                "maven-test-lib-1.0-classifier.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier2, jar));
    }

    @Test
    void emptyClassifierWithOptionalDashClassifier() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", "");
        assertEquals(
                "maven-test-lib-1.0.jar",
                FileNameMapping.evaluate("@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@", jar));
    }

    @Test
    void blankClassifierWithOptionalDashClassifier() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", " ");
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    @Test
    void classifierIsTrimmedInDashClassifier() throws Exception {
        Artifact jar = artifact("org.apache.sample", "maven-test-lib", "1.0", " tests ");
        assertEquals("maven-test-lib-1.0-tests.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
    }

    @Test
    void snapshotUsesBaseVersionByDefault() throws Exception {
        ArtifactStub jar = artifact("org.apache.sample", "maven-test-lib", "1.0-20261026.101010-1", null);
        jar.setBaseVersion("1.0-SNAPSHOT");
        assertEquals("maven-test-lib-1.0-SNAPSHOT.jar", FileNameMapping.evaluate(DEFAULT_MAPPING, jar));
        assertEquals(
                "maven-test-lib-1.0-20261026.101010-1.jar",
                FileNameMapping.evaluate("@{artifactId}@-@{version}@.@{extension}@", jar));
    }

    private static ArtifactStub artifact(String groupId, String artifactId, String version, String classifier) {
        ArtifactStub artifact = new ArtifactStub();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setVersion(version);
        artifact.setBaseVersion(version);
        artifact.setClassifier(classifier);
        artifact.setExtension("jar");
        return artifact;
    }
}
