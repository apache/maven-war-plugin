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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * comprehensive test on buildExplodedWebApp is done on WarExplodedMojoTest
 */
@MojoTest
public class WarMojoTest {

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWar")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWar-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/xml-config/web.xml")
    @Test
    public void testSimpleWar(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value =
                    "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-output")
    @MojoParameter(
            name = "webXml",
            value =
                    "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "packagingIncludes", value = "%regex[(.(?!exile))+]")
    @Test
    public void testSimpleWarPackagingExcludeWithIncludesRegEx(WarMojo mojo) throws Exception {

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {
                    null, mojo.getWebXml().getName(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-output")
    @MojoParameter(
            name = "webXml",
            value =
                    "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "packagingExcludes", value = "%regex[.+/last-exile.+]")
    @Test
    public void testSimpleWarPackagingExcludesWithRegEx(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {
                    null, mojo.getWebXml().getName(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/Classifier-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/Classifier-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/Classifier")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/Classifier-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/Classifier-test-data/xml-config/web.xml")
    @MojoParameter(name = "classifier", value = "test-classifier")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testClassifier(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple-test-classifier.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/PrimaryArtifact-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/PrimaryArtifact-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/PrimaryArtifact")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/PrimaryArtifact-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/PrimaryArtifact-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testPrimaryArtifact(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        warArtifact.setFile(new File("error.war"));
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/not-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testNotPrimaryArtifact(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        warArtifact.setFile(new File("error.war"));
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContent(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "META-INF/config.xml",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/xml-config/web.xml")
    @MojoParameter(
            name = "containerConfigXML",
            value =
                    "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/source/META-INF/config.xml")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContentWithContainerConfig(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "META-INF/config.xml",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, null, mojo.getWebXml().getName(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value =
                    "target/test-classes/unit/warmojotest/WarWithManifestMultibyteSymbolTransferredToNewLineAsWhole-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value =
                    "target/test-classes/unit/warmojotest/WarWithManifestMultibyteSymbolTransferredToNewLineAsWhole-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warmojotest/WarWithManifestMultibyteSymbolTransferredToNewLineAsWhole")
    @MojoParameter(
            name = "outputDirectory",
            value =
                    "target/test-classes/unit/warmojotest/WarWithManifestMultibyteSymbolTransferredToNewLineAsWhole-output")
    @MojoParameter(
            name = "webXml",
            value =
                    "target/test-classes/unit/warmojotest/WarWithManifestMultibyteSymbolTransferredToNewLineAsWhole-test-data/xml-config/web.xml")
    @MojoParameter(name = "warName", value = "simple")
    @SuppressWarnings("checkstyle:all")
    @Disabled(
            "Quarantined until the manifest entries are fixed to be valid according to the Jar File Specification and IETF RFC 3629")
    @Test
    public void
            warMojo_ShouldMakeWarWithManifestMultibyteSymbolTransferredToNewLineAsWhole_WhenManifestEntriesContainLongMultibyteString(
                    WarMojo mojo) throws Exception {
        // given
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        Map<String, String> manifestEntries = new HashMap<>();
        manifestEntries.put("Build-Jdk-Spec", "27");
        manifestEntries.put(
                "My-Custom-String", "αααααααααααααααααααααααααααββββββββββββββββββββββββββββββββββββγγγγγγγγγγγγγγγ");

        mojo.getArchive().setManifestEntries(manifestEntries);

        // when
        mojo.execute();

        // then
        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "META-INF/config.xml",
                    "WEB-INF/web.xml",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {
                    "Manifest-Version: 1.0\n" + "Created-By: Maven WAR Plugin\n"
                            + "My-Custom-String: ααααααααααααααααααααααααααα\n"
                            + " ββββββββββββββββββββββββββββββββββββ\n"
                            + " γγγγγγγγγγγγγγγ\n"
                            + "Build-Jdk-Spec: 27\n\n",
                    null,
                    mojo.getWebXml().getName(),
                    null,
                    null,
                    null,
                    null
                });
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-output")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/xml-config/web.xml")
    @MojoParameter(name = "failOnMissingWebXml", value = "false")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlFalse(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple.war");
        final Map<String, JarEntry> jarContent = assertJarContent(
                expectedJarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, null, null, null, null});

        assertFalse(jarContent.containsKey("WEB-INF/web.xml"), "web.xml should be missing");
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "failOnMissingWebXml", value = "true")
    @Test
    public void testFailOnMissingWebXmlTrue(WarMojo mojo) throws Exception {

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because web.xml is missing");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30Used(WarMojo mojo) throws Exception {
        JarArtifactStub jarArtifactStub = createServletApi3JarArtifact();
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(jarArtifactStub);
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setProject(project);

        mojo.execute();

        // validate war file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedWarFile = new File(outputDir, "simple.war");
        final Map<String, JarEntry> jarContent = assertJarContent(
                expectedWarFile,
                new String[] {
                    "META-INF/MANIFEST.MF",
                    "pansit.jsp",
                    "org/web/app/last-exile.jsp",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {null, null, null, null, null});

        assertFalse(jarContent.containsKey("WEB-INF/web.xml"), "web.xml should be missing");
    }

    private JarArtifactStub createServletApi3JarArtifact() {
        DefaultArtifactHandler jarArtifactHandler = new DefaultArtifactHandler("jar");
        jarArtifactHandler.setAddedToClasspath(true);
        JarArtifactStub jarArtifactStub = new JarArtifactStub(getBasedir(), jarArtifactHandler);
        jarArtifactStub.setFile(
                new File(getBasedir(), "/target/test-classes/unit/sample_wars/javax.servlet-api-3.0.1.jar"));
        jarArtifactStub.setScope(Artifact.SCOPE_PROVIDED);
        return jarArtifactStub;
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarNotUnderServlet30-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarNotUnderServlet30-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarNotUnderServlet30")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarNotUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30NotUsed(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setProject(project);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because no 'failOnMissingWebXml' policy was set and the project "
                    + "does not depend on Servlet 3.0");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/AttachClasses")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/AttachClasses-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "attachClasses", value = "true")
    @MojoParameter(name = "classesClassifier", value = "classes")
    @Test
    public void testAttachClasses(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple-classes.jar");
        assertJarContent(
                expectedJarFile, new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}, new String[] {null, null
                });
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-test-data/xml-config/web.xml")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "attachClasses", value = "true")
    @MojoParameter(name = "classesClassifier", value = "mystuff")
    @Test
    public void testAttachClassesWithCustomClassifier(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple-mystuff.jar");
        assertJarContent(
                expectedJarFile, new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}, new String[] {null, null
                });
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/IncludeEmptyDirectories-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/IncludeEmptyDirectories-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/IncludeEmptyDirectories")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/IncludeEmptyDirectories-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "includeEmptyDirectories", value = "true")
    @Test
    public void testIncludeEmptyDirectories(WarMojo mojo) throws Exception {
        // Git cannot hold an empty directory, so it is created here
        File logs =
                new File((File) MojoExtension.getVariableValueFromObject(mojo, "warSourceDirectory"), "WEB-INF/logs");
        assertTrue(logs.isDirectory() || logs.mkdirs(), "could not create " + logs);

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        assertJarContent(
                new File(outputDir, "simple.war"),
                new String[] {"WEB-INF/web.xml", "WEB-INF/logs/"},
                new String[] {null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/Skip")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/Skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "skip", value = "true")
    @Test
    public void testSkip(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        File webappDirectory = new File(MojoExtension.getVariableValueFromObject(mojo, "webappDirectory")
                .toString());
        File outputDirectory = new File(MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString());
        assertFalse(webappDirectory.exists(), "exploded directory created: " + webappDirectory);
        assertFalse(new File(outputDirectory, "simple.war").exists(), "war file created");
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/ArchiveClasses")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/ArchiveClasses-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "archiveClasses", value = "true")
    @Test
    public void testArchiveClasses(WarMojo mojo) throws Exception {
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(createJarArtifact("compiled", Artifact.SCOPE_COMPILE));
        project.addArtifact(createJarArtifact("provided", Artifact.SCOPE_PROVIDED));
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setProject(project);
        mojo.execute();

        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedWarFile = new File(outputDir, "simple.war");
        Map<String, JarEntry> content = assertJarContent(
                expectedWarFile,
                new String[] {
                    "pansit.jsp",
                    "WEB-INF/web.xml",
                    "WEB-INF/lib/maven-war-plugin-test-0.0-Test.jar",
                    "WEB-INF/lib/compiled-0.0-Test.jar"
                },
                new String[] {null, null, null, null});
        assertFalse(content.containsKey("WEB-INF/classes/sample-servlet.clazz"), "classes were not archived");
        assertFalse(content.containsKey("WEB-INF/lib/provided-0.0-Test.jar"), "provided dependency packaged");
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/AttachArchivedClasses")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/AttachArchivedClasses-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "archiveClasses", value = "true")
    @MojoParameter(name = "attachClasses", value = "true")
    @MojoParameter(name = "classesClassifier", value = "classes")
    @Test
    public void testAttachArchivedClasses(WarMojo mojo) throws Exception {
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        assertJarContent(
                new File(outputDir, "simple-classes.jar"),
                new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"},
                new String[] {null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/ManifestEntries")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/ManifestEntries-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testManifestDefaultEntries(WarMojo mojo) throws Exception {
        mojo.getArchive().setForced(true);
        mojo.getArchive().getManifest().setAddDefaultSpecificationEntries(true);
        mojo.getArchive().getManifest().setAddDefaultImplementationEntries(true);
        // the Specification-Version entry is derived from the selected version of the project artifact
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir()) {
            @Override
            public ArtifactVersion getSelectedVersion() {
                return new DefaultArtifactVersion("1.0-SNAPSHOT");
            }
        };
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        try (JarFile war = new JarFile(new File(outputDir, "simple.war"))) {
            Attributes manifest = war.getManifest().getMainAttributes();
            assertEquals("Test Project", manifest.getValue(Attributes.Name.SPECIFICATION_TITLE));
            assertEquals("1.0", manifest.getValue(Attributes.Name.SPECIFICATION_VERSION));
            assertEquals("Test Name", manifest.getValue(Attributes.Name.SPECIFICATION_VENDOR));
            assertEquals("Test Project", manifest.getValue(Attributes.Name.IMPLEMENTATION_TITLE));
            assertEquals("0.0-Test", manifest.getValue(Attributes.Name.IMPLEMENTATION_VERSION));
            assertEquals("Test Name", manifest.getValue(Attributes.Name.IMPLEMENTATION_VENDOR));
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarUnderJakarta")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderJakarta-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlNotSpecifiedAndJakartaServletUsed(WarMojo mojo, @TempDir File tempDir)
            throws Exception {
        JarArtifactStub jakartaApi = createJarArtifact("jakarta.servlet-api", Artifact.SCOPE_PROVIDED);
        jakartaApi.setGroupId("jakarta.servlet");
        jakartaApi.setFile(createJakartaServletApiJar(tempDir));
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(jakartaApi);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setProject(project);

        mojo.execute();

        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        Map<String, JarEntry> content =
                assertJarContent(new File(outputDir, "simple.war"), new String[] {"pansit.jsp"}, new String[] {null});
        assertFalse(content.containsKey("WEB-INF/web.xml"), "web.xml should be missing");
    }

    private JarArtifactStub createJarArtifact(String artifactId, String scope) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
        handler.setAddedToClasspath(true);
        JarArtifactStub artifact = new JarArtifactStub(getBasedir(), handler);
        artifact.setArtifactId(artifactId);
        artifact.setFile(new File(getBasedir(), "/target/test-classes/unit/sample_wars/simple.jar"));
        artifact.setScope(scope);
        return artifact;
    }

    /**
     * Creates a jar that holds only the annotation type the plugin looks for to detect Servlet 3.0 or newer.
     */
    private static File createJakartaServletApiJar(File directory) throws IOException {
        String className = "jakarta/servlet/annotation/WebServlet";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0xCAFEBABE);
            out.writeShort(0); // minor version
            out.writeShort(52); // Java 8
            out.writeShort(7); // constant pool count
            out.writeByte(1); // #1 Utf8 this class
            out.writeUTF(className);
            out.writeByte(7); // #2 Class #1
            out.writeShort(1);
            out.writeByte(1); // #3 Utf8 super class
            out.writeUTF("java/lang/Object");
            out.writeByte(7); // #4 Class #3
            out.writeShort(3);
            out.writeByte(1); // #5 Utf8 interface
            out.writeUTF("java/lang/annotation/Annotation");
            out.writeByte(7); // #6 Class #5
            out.writeShort(5);
            out.writeShort(0x2601); // public abstract interface annotation
            out.writeShort(2); // this class
            out.writeShort(4); // super class
            out.writeShort(1); // interfaces count
            out.writeShort(6);
            out.writeShort(0); // fields
            out.writeShort(0); // methods
            out.writeShort(0); // attributes
        }
        File jar = new File(directory, "jakarta.servlet-api-5.0.0.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry(className + ".class"));
            out.write(bytes.toByteArray());
            out.closeEntry();
        }
        return jar;
    }

    private Map<String, JarEntry> assertJarContent(
            final File expectedJarFile, final String[] files, final String[] filesContent) throws IOException {
        return assertJarContent(expectedJarFile, files, filesContent, null);
    }

    private Map<String, JarEntry> assertJarContent(
            final File expectedJarFile,
            final String[] files,
            final String[] filesContent,
            final String[] mustNotBeInJar)
            throws IOException {
        // Sanity check
        assertEquals(files.length, filesContent.length, "Could not test, files and filesContent length does not match");

        assertTrue(expectedJarFile.exists(), "war file not created: " + expectedJarFile.toString());
        final Map<String, JarEntry> jarContent = new HashMap<>();
        try (JarFile jarFile = new JarFile(expectedJarFile)) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                Object previousValue = jarContent.put(entry.getName(), entry);
                assertNull(previousValue, "Duplicate Entry in Jar File: " + entry.getName());
            }

            for (int i = 0; i < files.length; i++) {
                String file = files[i];

                assertTrue(jarContent.containsKey(file), "File[" + file + "] not found in archive");
                if (filesContent[i] != null) {
                    assertEquals(
                            filesContent[i],
                            IOUtil.toString(jarFile.getInputStream(jarContent.get(file))),
                            "Content of file[" + file + "] does not match");
                }
            }
            if (mustNotBeInJar != null) {
                for (String file : mustNotBeInJar) {
                    assertFalse(jarContent.containsKey(file), "File[" + file + "]  found in archive");
                }
            }
            return jarContent;
        }
    }
}
