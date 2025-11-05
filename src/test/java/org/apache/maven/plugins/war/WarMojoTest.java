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
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ProjectHelperStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.BeforeEach;
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
    //    protected static final File OVERLAYS_TEMP_DIR = new File(getBasedir(), "target/test-overlays/");
    //    protected static final File OVERLAYS_ROOT_DIR = new File(getBasedir(), "target/test-classes/overlays/");
    protected static final String MANIFEST_PATH = "META-INF" + File.separator + "MANIFEST.MF";

    //    private static File pomFile =
    //            new File(getBasedir(), "target/test-classes/unit/warmojotest/plugin-config-primary-artifact.xml");

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warmojotest");
    }

    @BeforeEach
    public void setUp() throws Exception {
        //        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
        //                .setSystemProperties(System.getProperties())
        //                .setStartTime(new Date());

        //        MavenSession mavenSession =
        //                new MavenSession((PlexusContainer) null, (RepositorySystemSession) null, request, null);
        //        getContainer().addComponent(mavenSession, MavenSession.class.getName());
        //        mojo = (WarMojo) lookupMojo("war", pomFile);
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWar-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testSimpleWar(WarMojo mojo) throws Exception {
        String testId = "SimpleWar";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "packagingIncludes", value = "%regex[(.(?!exile))+]")
    @Test
    public void testSimpleWarPackagingExcludeWithIncludesRegEx(WarMojo mojo) throws Exception {
        String testId = "SimpleWarPackagingExcludeWithIncludesRegEx";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {
                    null, mojo.getWebXml().toString(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }
    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludesWithRegEx-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "packagingExcludes", value = "%regex[.+/last-exile.+]")
    @Test
    public void testSimpleWarPackagingExcludesWithRegEx(WarMojo mojo) throws Exception {
        String testId = "SimpleWarPackagingExcludesWithRegEx";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                    "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties"
                },
                new String[] {
                    null, mojo.getWebXml().toString(), null, null, null,
                },
                new String[] {"org/web/app/last-exile.jsp"});
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/Classifier-output")
    @MojoParameter(name = "classifier", value = "test-classifier")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testClassifier(WarMojo mojo) throws Exception {
        String testId = "Classifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @Provides
    private MavenProjectHelper projectHelper() {
        return new ProjectHelperStub();
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/PrimaryArtifact-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testPrimaryArtifact(WarMojo mojo) throws Exception {
        String testId = "PrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        warArtifact.setFile(new File("error.war"));
        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/not-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/NotPrimaryArtifact-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testNotPrimaryArtifact(WarMojo mojo) throws Exception {
        String testId = "NotPrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        warArtifact.setFile(new File("error.war"));
        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarWithMetaInfContent-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContent(WarMojo mojo) throws Exception {
        String testId = "SimpleWarWithMetaInfContent";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarWithContainerConfig-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testMetaInfContentWithContainerConfig(WarMojo mojo) throws Exception {
        String testId = "SimpleWarWithContainerConfig";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        // Create the sample config.xml
        final File configFile = new File(webAppSource, "META-INF/config.xml");
        createFile(configFile, "<config></config>");

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlFalse-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlFalse(WarMojo mojo) throws Exception {
        String testId = "SimpleWarMissingWebXmlFalse";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarMissingWebXmlTrue-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlTrue(WarMojo mojo) throws Exception {
        String testId = "SimpleWarMissingWebXmlTrue";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setFailOnMissingWebXml(true);

        try {
            mojo.execute();
            fail("Building of the war isn't possible because web.xml is missing");
        } catch (MojoExecutionException e) {
            // expected behaviour
        }
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    @Disabled // TODO test failed and error message corresponed to the test case description
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30Used(WarMojo mojo) throws Exception {
        String testId = "SimpleWarUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        JarArtifactStub jarArtifactStub = new JarArtifactStub(getBasedir(), artifactHandler);
        jarArtifactStub.setFile(
                new File(getBasedir(), "/target/test-classes/unit/sample_wars/javax.servlet-api-3.0.1.jar"));
        jarArtifactStub.setScope(Artifact.SCOPE_PROVIDED);
        project.addArtifact(jarArtifactStub);

        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);

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

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/SimpleWarUnderServlet30-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30NotUsed(WarMojo mojo) throws Exception {
        String testId = "SimpleWarNotUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        project.setArtifact(warArtifact);
        project.setFile(warArtifact.getFile());
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
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
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/AttachClasses-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testAttachClasses(WarMojo mojo) throws Exception {
        String testId = "AttachClasses";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("classes");

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File(outputDir, "simple-classes.jar");
        assertJarContent(
                expectedJarFile
                        , new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}
                        , new String[] {null, null}
        );
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warmojotest/plugin-config-primary-artifact.xml")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warmojotest/AttachClassesCustomClassifier-output")
    @MojoParameter(name = "warName", value = "simple")
    @Test
    public void testAttachClassesWithCustomClassifier(WarMojo mojo) throws Exception {
        String testId = "AttachClassesCustomClassifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        project.setArtifact(warArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        mojo.setAttachClasses(true);
        mojo.setClassesClassifier("mystuff");

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File(outputDir, "simple-mystuff.jar");
        assertJarContent(expectedJarFile
                , new String[] {"META-INF/MANIFEST.MF", "sample-servlet.clazz"}
                , new String[] {null, null}
        );
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

    /**
     * initialize required parameters
     *
     * @param mojo The mojo to be tested.
     * @param classesDir The classes' directory.
     * @param webAppSource The webAppSource.
     * @param webAppDir The webAppDir folder.
     * @param project The Maven project.
     * @throws Exception in case of errors
     */
    @MojoParameter(name = "outdatedCheckPath", value = "WEB-INF/lib/")
    protected void configureMojo(
            AbstractWarMojo mojo, File classesDir, File webAppSource, File webAppDir, MavenProjectBasicStub project)
            throws Exception {
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDir);
        mojo.setProject(project);
    }

    /**
     * create an isolated xml dir
     *
     * @param id The id.
     * @param xmlFiles array of xml files.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    protected File createXMLConfigDir(String id, String[] xmlFiles) throws Exception {
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
    protected File getWebAppSource(String id) throws Exception {
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
    protected File createWebAppSource(String id, boolean createSamples) throws Exception {
        File webAppSource = getWebAppSource(id);
        if (createSamples) {
            File simpleJSP = new File(webAppSource, "pansit.jsp");
            File jspFile = new File(webAppSource, "org/web/app/last-exile.jsp");

            createFile(simpleJSP);
            createFile(jspFile);
        }
        return webAppSource;
    }

    protected File createWebAppSource(String id) throws Exception {
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
    protected File createClassesDir(String id, boolean empty) throws Exception {
        File classesDir = new File(getTestDirectory() + "/" + id + "-test-data/classes/");

        createDir(classesDir);

        if (!empty) {
            createFile(new File(classesDir + "/sample-servlet.clazz"));
        }

        return classesDir;
    }

    protected void createDir(File dir) {
        if (!dir.exists()) {
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir.toString());
        }
    }

    protected void createFile(File testFile, String body) throws Exception {
        createDir(testFile.getParentFile());
        FileUtils.fileWrite(testFile.toString(), body);

        assertTrue(testFile.exists(), "could not create file: " + testFile);
    }

    protected void createFile(File testFile) throws Exception {
        createFile(testFile, testFile.toString());
    }

    //    /**
    //     * Generates test war.
    //     * Generates war with such a structure:
    //     * <ul>
    //     * <li>jsp
    //     * <ul>
    //     * <li>d
    //     * <ul>
    //     * <li>a.jsp</li>
    //     * <li>b.jsp</li>
    //     * <li>c.jsp</li>
    //     * </ul>
    //     * </li>
    //     * <li>a.jsp</li>
    //     * <li>b.jsp</li>
    //     * <li>c.jsp</li>
    //     * </ul>
    //     * </li>
    //     * <li>WEB-INF
    //     * <ul>
    //     * <li>classes
    //     * <ul>
    //     * <li>a.clazz</li>
    //     * <li>b.clazz</li>
    //     * <li>c.clazz</li>
    //     * </ul>
    //     * </li>
    //     * <li>lib
    //     * <ul>
    //     * <li>a.jar</li>
    //     * <li>b.jar</li>
    //     * <li>c.jar</li>
    //     * </ul>
    //     * </li>
    //     * <li>web.xml</li>
    //     * </ul>
    //     * </li>
    //     * </ul>
    //     * Each of the files will contain: id+'-'+path
    //     *
    //     * @param id the id of the overlay containing the full structure
    //     * @return the war file
    //     * @throws Exception if an error occurs
    //     */
    //    protected File generateFullOverlayWar(String id) throws Exception {
    //        final File destFile = new File(OVERLAYS_TEMP_DIR, id + ".war");
    //        if (destFile.exists()) {
    //            return destFile;
    //        }
    //
    //        // Archive was not yet created for that id so let's create it
    //        final File rootDir = new File(OVERLAYS_ROOT_DIR, id);
    //        rootDir.mkdirs();
    //        String[] filePaths = new String[] {
    //            "jsp/d/a.jsp",
    //            "jsp/d/b.jsp",
    //            "jsp/d/c.jsp",
    //            "jsp/a.jsp",
    //            "jsp/b.jsp",
    //            "jsp/c.jsp",
    //            "WEB-INF/classes/a.clazz",
    //            "WEB-INF/classes/b.clazz",
    //            "WEB-INF/classes/c.clazz",
    //            "WEB-INF/lib/a.jar",
    //            "WEB-INF/lib/b.jar",
    //            "WEB-INF/lib/c.jar",
    //            "WEB-INF/web.xml"
    //        };
    //
    //        for (String filePath : filePaths) {
    //            createFile(new File(rootDir, filePath), id + "-" + filePath);
    //        }
    //
    //        createArchive(rootDir, destFile);
    //        return destFile;
    //    }
    //
    //    /**
    //     * Builds a test overlay.
    //     *
    //     * @param id the id of the overlay (see test/resources/overlays)
    //     * @return a test war artifact with the content of the given test overlay
    //     */
    //    protected ArtifactStub buildWarOverlayStub(String id) {
    //        // Create war file
    //        final File destFile = new File(OVERLAYS_TEMP_DIR, id + ".war");
    //        if (!destFile.exists()) {
    //            createArchive(new File(OVERLAYS_ROOT_DIR, id), destFile);
    //        }
    //
    //        return new WarOverlayStub(getBasedir(), id, destFile);
    //    }
    //
    //    protected File getOverlayFile(String id, String filePath) {
    //        final File overlayDir = new File(OVERLAYS_ROOT_DIR, id);
    //        final File file = new File(overlayDir, filePath);
    //
    //        // Make sure the file exists
    //        assertTrue(
    //                file.exists(),
    //                "Overlay file " + filePath + " does not exist for overlay " + id + " at " +
    // file.getAbsolutePath());
    //        return file;
    //    }

    protected void createArchive(final File directory, final File destinationFile) {
        try {
            JarArchiver archiver = new JarArchiver();

            archiver.setDestFile(destinationFile);
            archiver.addDirectory(directory);

            archiver.createArchive();

        } catch (ArchiverException e) {
            e.printStackTrace();
            fail("Failed to create overlay archive " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception " + e.getMessage());
        }
    }
}
