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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the mapping of file names.
 */
class FileNameMappingTest {

    @Test
    void completeMapping() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample",
                "maven-test-lib",
                "1.0",
                "compile",
                "jar",
                null,
                new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0.jar", FileNameMapping.evaluate("@{artifactId}@-@{version}@.@{extension}@", jar));
    }

    @Test
    void noVersionMapping() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", null, new DefaultArtifactHandler("jar"));
        assertEquals("maven-test-lib.jar", FileNameMapping.evaluate("@{artifactId}@.@{extension}@", jar));
    }

    @Test
    void mappingWithGroupId() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", null, new DefaultArtifactHandler("jar"));
        assertEquals(
                "org.apache.sample-maven-test-lib-1.0.jar",
                FileNameMapping.evaluate("@{groupId}@-@{artifactId}@-@{version}@.@{extension}@", jar));
    }

    @Test
    void mappingWithClassifier() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample",
                "maven-test-lib",
                "1.0",
                null,
                "jar",
                "classifier",
                new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0-classifier.jar",
                FileNameMapping.evaluate(FileNameMapping.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar));
    }

    @Test
    void mappingWithNullClassifier() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", null, new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0.jar",
                FileNameMapping.evaluate(FileNameMapping.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar));
    }

    /**
     * Test for #65. When classifier is null, the default classifier mapping should
     * produce a clean filename without a trailing dash.
     */
    @Test
    void mappingWithNullClassifierShouldNotHaveTrailingDash() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", null, new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0.jar",
                FileNameMapping.evaluate(FileNameMapping.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar));
    }

    @Test
    void mappingWithEmptyClassifierShouldNotHaveTrailingDash() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", "", new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0.jar",
                FileNameMapping.evaluate(FileNameMapping.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar));
    }

    /**
     * Test for MWAR-212.
     */
    @Test
    void mappingWithOptionalClassifier() throws Exception {
        final String mappingWithOptionalClassifier1 = "@{artifactId}@-@{version}@@{dashClassifier}@.@{extension}@";
        final String mappingWithOptionalClassifier2 = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", null, new DefaultArtifactHandler("jar"));
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier1, jar));
        assertEquals("maven-test-lib-1.0.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier2, jar));

        jar = new DefaultArtifact(
                "org.apache.sample",
                "maven-test-lib",
                "1.0",
                null,
                "jar",
                "classifier",
                new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0-classifier.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier1, jar));
        assertEquals(
                "maven-test-lib-1.0-classifier.jar", FileNameMapping.evaluate(mappingWithOptionalClassifier2, jar));
    }

    @Test
    void emptyClassifierWithOptionalDashClassifier() throws Exception {
        Artifact jar = new DefaultArtifact(
                "org.apache.sample", "maven-test-lib", "1.0", null, "jar", "", new DefaultArtifactHandler("jar"));
        assertEquals(
                "maven-test-lib-1.0.jar",
                FileNameMapping.evaluate("@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@", jar));
    }
}
