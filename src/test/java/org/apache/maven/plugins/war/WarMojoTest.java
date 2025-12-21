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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.ProjectHelperStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * comprehensive test on buildExplodedWebApp is done on WarExplodedMojoTest
 */
class WarMojoTest extends AbstractWarMojoTest {
    WarMojo mojo;

    private static File pomFile =
            new File(getBasedir(), "target/test-classes/unit/warmojotest/plugin-config-primary-artifact.xml");

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warmojotest");
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mojo = (WarMojo) lookupMojo("war", pomFile);
    }

    @Test
    void simpleWar() throws Exception {
        String testId = "SimpleWar";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        mojo.execute();

        // validate jar file
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

    @Test
    void simpleWarPackagingExcludeWithIncludesRegEx() throws Exception {
        String testId = "SimpleWarPackagingExcludeWithIncludesRegEx";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        setVariableValueToObject(mojo, "packagingIncludes", "%regex[(.(?!exile))+]");
        //        setVariableValueToObject( mojo, "packagingIncludes", "%regex" );

        mojo.execute();

        // validate jar file
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
                    null, mojo.getWebXml().toString(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }

    @Test
    void simpleWarPackagingExcludesWithRegEx() throws Exception {
        String testId = "SimpleWarPackagingExcludesWithRegEx";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        setVariableValueToObject(mojo, "packagingExcludes", "%regex[.+/last-exile.+]");

        mojo.execute();

        // validate jar file
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
                    null, mojo.getWebXml().toString(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }

    @Test
    void classifier() throws Exception {
        String testId = "Classifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "projectHelper", projectHelper);
        setVariableValueToObject(mojo, "classifier", "test-classifier");
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        mojo.execute();

        // validate jar file
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
                new String[] {null, mojo.getWebXml().toString(), null, null, null, null});
    }

    @Test
    void primaryArtifact() throws Exception {
        String testId = "PrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        warArtifact.setFile(new File("error.war"));
        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "projectHelper", projectHelper);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        mojo.execute();

        // validate jar file
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

    @Test
    void notPrimaryArtifact() throws Exception {
        // use a different pom
        File pom = new File(getBasedir(), "target/test-classes/unit/warmojotest/not-primary-artifact.xml");
        mojo = (WarMojo) lookupMojo("war", pom);

        String testId = "NotPrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        warArtifact.setFile(new File("error.war"));
        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "projectHelper", projectHelper);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        mojo.execute();

        // validate jar file
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

    @Test
    void metaInfContent() throws Exception {
        String testId = "SimpleWarWithMetaInfContent";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

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

    @Test
    void metaInfContentWithContainerConfig() throws Exception {
        String testId = "SimpleWarWithContainerConfig";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
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

    @Test
    void failOnMissingWebXmlFalse() throws Exception {

        String testId = "SimpleWarMissingWebXmlFalse";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setFailOnMissingWebXml(false);
        mojo.execute();

        // validate jar file
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

    @Test
    void failOnMissingWebXmlTrue() throws Exception {

        String testId = "SimpleWarMissingWebXmlTrue";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setFailOnMissingWebXml(true);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because web.xml is missing");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @Test
    void failOnMissingWebXmlNotSpecifiedAndServlet30Used() throws Exception {
        String testId = "SimpleWarUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        JarArtifactStub jarArtifactStub = new JarArtifactStub(getBasedir(), artifactHandler);
        jarArtifactStub.setFile(
                new File(getBasedir(), "/target/test-classes/unit/sample_wars/javax.servlet-api-3.0.1.jar"));
        jarArtifactStub.setScope(Artifact.SCOPE_PROVIDED);
        project.addArtifact(jarArtifactStub);

        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);

        mojo.execute();

        // validate war file
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

    @Test
    void failOnMissingWebXmlNotSpecifiedAndServlet30NotUsed() throws Exception {
        String testId = "SimpleWarNotUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because no 'failOnMissingWebXml' policy was set and the project "
                    + "does not depend on Servlet 3.0");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @Test
    void attachClasses() throws Exception {
        String testId = "AttachClasses";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("classes");

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File(outputDir, "simple-classes.jar");
        assertJarContent(
                expectedJarFile, new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}, new String[] {null, null
                });
    }

    @Test
    void attachClassesWithCustomClassifier() throws Exception {
        String testId = "AttachClassesCustomClassifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        String warName = "simple";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "outputDirectory", outputDir);
        setVariableValueToObject(mojo, "warName", warName);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("mystuff");

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File(outputDir, "simple-mystuff.jar");
        assertJarContent(
                expectedJarFile, new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}, new String[] {null, null
                });
    }

    protected Map<String, JarEntry> assertJarContent(
            final File expectedJarFile, final String[] files, final String[] filesContent) throws IOException {
        return assertJarContent(expectedJarFile, files, filesContent, null);
    }

    protected Map<String, JarEntry> assertJarContent(
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
