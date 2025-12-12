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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.ProjectHelperStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.Disabled;
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

    @Inject
    private MavenProjectHelper projectHelper;

    @Inject
    private ArtifactHandler artifactHandler;

    @Provides
    private MavenProjectHelper projectHelper() {
        return new ProjectHelperStub();
    }

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
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testNotPrimaryArtifact(WarMojo mojo) throws Exception {
        String testId = "NotPrimaryArtifact";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        warArtifact.setFile(new File("error.war"));
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory, xmlSource);

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
                new String[] {null, mojo.getWebXml().toString(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContent(WarMojo mojo) throws Exception {
        String testId = "SimpleWarWithMetaInfContent";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory, xmlSource);

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
                new String[] {null, null, mojo.getWebXml().toString(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContentWithContainerConfig(WarMojo mojo) throws Exception {
        String testId = "SimpleWarWithContainerConfig";
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory, xmlSource);
        mojo.setContainerConfigXML(configFile);

        mojo.execute();

        // validate jar file
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
                new String[] {null, null, mojo.getWebXml().toString(), null, null, null, null});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlFalse(WarMojo mojo) throws Exception {
        String testId = "SimpleWarMissingWebXmlFalse";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory);
        mojo.setFailOnMissingWebXml(false);
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
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlTrue(WarMojo mojo) throws Exception {
        String testId = "SimpleWarMissingWebXmlTrue";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory);
        mojo.setFailOnMissingWebXml(true);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because web.xml is missing");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    @Disabled // TODO test failed and error message corresponed to the test case description
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30Used(WarMojo mojo) throws Exception {
        String testId = "SimpleWarUnderServlet30";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        JarArtifactStub jarArtifactStub = createServletApi3JarArtifact();

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(jarArtifactStub);
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        configureMojo(mojo, project, classesDir, webAppSource, webAppDirectory);

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
        JarArtifactStub jarArtifactStub = new JarArtifactStub(getBasedir(), artifactHandler);
        jarArtifactStub.setFile(
                new File(getBasedir(), "/target/test-classes/unit/sample_wars/javax.servlet-api-3.0.1.jar"));
        jarArtifactStub.setScope(Artifact.SCOPE_PROVIDED);
        return jarArtifactStub;
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30NotUsed(WarMojo mojo) throws Exception {
        String testId = "SimpleWarNotUnderServlet30";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        configureMojo(mojo, project, classesDir, webAppSource, webAppDirectory);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because no 'failOnMissingWebXml' policy was set and the project "
                    + "does not depend on Servlet 3.0");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/AttachClasses-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testAttachClasses(WarMojo mojo) throws Exception {
        String testId = "AttachClasses";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory, xmlSource);
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("classes");

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
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testAttachClassesWithCustomClassifier(WarMojo mojo) throws Exception {
        String testId = "AttachClassesCustomClassifier";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory, xmlSource);
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("mystuff");

        mojo.execute();

        // validate jar file
        String outputDir = MojoExtension.getVariableValueFromObject(mojo, "outputDirectory")
                .toString();
        File expectedJarFile = new File(outputDir, "simple-mystuff.jar");
        assertJarContent(
                expectedJarFile, new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}, new String[] {null, null
                });
    }

    private void configureMojo(
            WarMojo mojo,
            WarArtifact4CCStub warArtifact,
            File classesDir,
            File webAppSource,
            File webAppDirectory,
            File xmlSource)
            throws Exception {
        configureMojo(mojo, warArtifact, classesDir, webAppSource, webAppDirectory);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
    }

    private void configureMojo(
            WarMojo mojo, MavenProjectArtifactsStub project, File classesDir, File webAppSource, File webAppDirectory) {
        mojo.setProject(project);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
    }

    private void configureMojo(
            WarMojo mojo, WarArtifact4CCStub warArtifact, File classesDir, File webAppSource, File webAppDirectory)
            throws Exception {
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
    }

    private File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warmojotest");
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

    /**
     * create an isolated xml dir
     *
     * @param id The id.
     * @param xmlFiles array of xml files.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    private File createXMLConfigDir(String id, String[] xmlFiles) throws Exception {
        File xmlConfigDir = new File(getTestDirectory(), "/" + id + "-test-data/xml-config");
        File xmlFile;

        createDir(xmlConfigDir);

        if (xmlFiles != null) {
            for (String o : xmlFiles) {
                xmlFile = new File(xmlConfigDir, o);
                createFile(xmlFile);
            }
        }

        return xmlConfigDir;
    }

    /**
     * Returns the webapp source directory for the specified id.
     *
     * @param id the id of the test
     * @return the source directory for that test
     * @throws Exception if an exception occurs
     */
    private File getWebAppSource(String id) throws Exception {
        return new File(getTestDirectory(), "/" + id + "-test-data/source");
    }

    /**
     * create an isolated web source with a sample jsp file
     *
     * @param id The id.
     * @param createSamples Create example files yes or no.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    private File createWebAppSource(String id, boolean createSamples) throws Exception {
        File webAppSource = getWebAppSource(id);
        if (createSamples) {
            File simpleJSP = new File(webAppSource, "pansit.jsp");
            File jspFile = new File(webAppSource, "org/web/app/last-exile.jsp");

            createFile(simpleJSP);
            createFile(jspFile);
        }
        return webAppSource;
    }

    private File createWebAppSource(String id) throws Exception {
        return createWebAppSource(id, true);
    }

    /**
     * create a class directory with or without a sample class
     *
     * @param id The id.
     * @param empty true to create a class files false otherwise.
     * @return The created class file.
     * @throws Exception in case of errors.
     */
    private File createClassesDir(String id, boolean empty) throws Exception {
        File classesDir = new File(getTestDirectory() + "/" + id + "-test-data/classes/");

        createDir(classesDir);

        if (!empty) {
            createFile(new File(classesDir + "/sample-servlet.clazz"));
        }

        return classesDir;
    }

    private void createDir(File dir) {
        if (!dir.exists()) {
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir.toString());
        }
    }

    private void createFile(File testFile, String body) throws Exception {
        createDir(testFile.getParentFile());
        FileUtils.fileWrite(testFile.toString(), body);

        assertTrue(testFile.exists(), "could not create file: " + testFile);
    }

    private void createFile(File testFile) throws Exception {
        createFile(testFile, testFile.toString());
    }
}
