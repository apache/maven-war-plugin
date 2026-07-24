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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.plugin.testing.stubs.ProjectStub;

/**
 * Stub
 */
public class MavenProjectBasicStub extends ProjectStub {

    public MavenProjectBasicStub() throws Exception {
        setGroupId("org.apache.maven.plugin.test");
        setArtifactId("maven-war-plugin-test");
        setVersion("0.0-Test");
        setName("Test Project ");
        setPackaging("jar");

        // Set a model with build directories
        String buildDir = System.getProperty("project.build.directory", "target");
        String outputDir = System.getProperty("project.build.outputDirectory", "target/classes");
        Model model = Model.newBuilder()
                .groupId("org.apache.maven.plugin.test")
                .artifactId("maven-war-plugin-test")
                .version("0.0-Test")
                .packaging("jar")
                .name("Test Project ")
                .description("Test Description")
                .build(Build.newBuilder()
                        .directory(buildDir)
                        .outputDirectory(outputDir)
                        .build())
                .build();
        setModel(model);

        // Set pomPath to a pom.xml file to satisfy PluginParameterExpressionEvaluatorV4
        // and MavenArchiver which both need a non-null pomPath.
        // Create the file in the project build directory (not /tmp) to avoid overriding
        // the basedir that MojoExtension resolves from @Basedir annotations.
        try {
            Path projectDir = Path.of(System.getProperty("user.dir"));
            Path targetDir = projectDir.resolve("target").resolve("test-stubs");
            Files.createDirectories(targetDir);
            Path pomFile = targetDir.resolve("test-pom.xml");
            if (!Files.exists(pomFile)) {
                Files.writeString(
                        pomFile,
                        "<project><modelVersion>4.0.0</modelVersion>"
                                + "<groupId>org.apache.maven.plugin.test</groupId>"
                                + "<artifactId>maven-war-plugin-test</artifactId>"
                                + "<version>0.0-Test</version></project>");
            }
            setPomPath(pomFile);
            // Only set basedir/rootDirectory if they are not already set (e.g., by MojoExtension)
            if (getBasedir() == null) {
                setBasedir(projectDir);
            }
            if (getRootDirectory() == null) {
                setRootDirectory(projectDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create pom.xml for test stub", e);
        }
    }
}
