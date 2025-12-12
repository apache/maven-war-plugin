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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.ProjectHelperStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.Test;

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
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWar-test-data/xml-config/web.xml")
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
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-test-data/xml-config/web.xml")
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
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-test-data/xml-config/web.xml")
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
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/Classifier-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/Classifier-test-data/xml-config/web.xml")
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
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/PrimaryArtifact-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/PrimaryArtifact-test-data/xml-config/web.xml")
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
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-test-data/xml-config/web.xml")
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
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-test-data/xml-config/web.xml")
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
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/xml-config/web.xml")
    @MojoParameter(name = "containerConfigXML", value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-test-data/source/META-INF/config.xml")
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
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-output")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-test-data/xml-config/web.xml")
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
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/AttachClasses-test-data/xml-config/web.xml")
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
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-output")
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
