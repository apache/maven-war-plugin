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
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.stub.AarArtifactStub;
import org.apache.maven.plugins.war.stub.EJBArtifactStub;
import org.apache.maven.plugins.war.stub.EJBArtifactStubWithClassifier;
import org.apache.maven.plugins.war.stub.EJBClientArtifactStub;
import org.apache.maven.plugins.war.stub.IncludeExcludeWarArtifactStub;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.PARArtifactStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.apache.maven.plugins.war.stub.TLDArtifactStub;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.apache.maven.plugins.war.stub.XarArtifactStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
public class WarExplodedMojoTest {

    @Inject
    private ArtifactHandler artifactHandler;

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir");
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar")
    @Test
    public void testSimpleExplodedWar(WarExplodedMojo mojo) throws Exception {
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(MojoExtension.getBasedir()
                + "/target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/resources/");
        mojo.setWebResources(resources);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWebResourceFile = new File(webAppDirectory, "pix/panis_na.jpg");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWebResourceFile.exists(), "resources doesn't exist: " + expectedWebResourceFile);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/SimpleExplodedWar")
    @Test
    public void testSimpleExplodedWarWTargetPath(WarExplodedMojo mojo) throws Exception {
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(MojoExtension.getBasedir()
                + "/target/test-classes/unit/warexplodedmojo/SimpleExplodedWar-test-data/resources/");
        resources[0].setTargetPath("targetPath");
        mojo.setWebResources(resources);

        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWebResourceFile = new File(webAppDirectory, "targetPath/pix/panis_na.jpg");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWebResourceFile.exists(), "resources doesn't exist: " + expectedWebResourceFile);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithCustomWebXML-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithCustomWebXML-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithCustomWebXML-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithCustomWebXML")
    @Test
    public void testExplodedWarWithCustomWebXML(WarExplodedMojo mojo) throws Exception {

        // configure mojo
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWEBXMLFile.exists(), "WEB XML not found: " + expectedWEBXMLFile);
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");
        assertEquals(mojo.getWebXml().getName(), FileUtils.fileRead(expectedWEBXMLFile), "WEB XML not correct");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedMETAINFDir.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithContainerConfigXML-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithContainerConfigXML-test-data/source/")
    @MojoParameter(
            name = "containerConfigXML",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithContainerConfigXML-test-data/xml-config/config.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithContainerConfigXML")
    @Test
    public void testExplodedWarWithContainerConfigXML(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedContainerConfigXMLFile = new File(webAppDirectory, "META-INF/config.xml");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(
                expectedContainerConfigXMLFile.exists(),
                "Container Config XML not found:" + expectedContainerConfigXMLFile);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedContainerConfigXMLFile.delete();
        expectedWEBINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithSimpleExternalWARFile-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithSimpleExternalWARFile-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithSimpleExternalWARFile")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/warexplodedmojo/test-dir/war/work-ExplodedWarWithSimpleExternalWARFile")
    @Test
    public void testExplodedWarWithSimpleExternalWARFile(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation - META-INF is automatically excluded so remove the file from the list
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedWARFile = new File(webAppDirectory, "/org/sample/company/test.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        // check simple.war in the unit test dir under resources to verify the list of files
        assertTrue(expectedWEBXMLFile.exists(), "web xml not found: " + expectedWEBXMLFile);
        assertTrue(expectedWARFile.exists(), "war file not found: " + expectedWARFile);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedWARFile.delete();
    }

    /**
     * Merge a dependent WAR when a file in the war source directory overrides one found in the WAR.
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarMergeWarLocalFileOverride-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarMergeWarLocalFileOverride-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarMergeWarLocalFileOverride")
    @MojoParameter(
            name = "workDirectory",
            value =
                    "target/test-classes/unit/warexplodedmojo/test-dir/war/work-ExplodedWarMergeWarLocalFileOverride")
    @Test
    public void testExplodedWarMergeWarLocalFileOverride(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(warArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedFile = new File(webAppDirectory, "/org/sample/company/test.jsp");

        assertTrue(expectedFile.exists(), "file not found: " + expectedFile);
        assertEquals("org/sample/company/test.jsp", FileUtils.fileRead(expectedFile), "file incorrect");

        // check when the merged war file is newer - so set an old time on the local file
        File simpleJSP = new File(MojoExtension.getBasedir(), "target/test-classes/unit/warexplodedmojo/testExplodedWarMergeWarLocalFileOverride-test-data/source/org/sample/company/test.jsp");
        long time =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2005-1-1").getTime();
        simpleJSP.setLastModified(time);
        mojo.execute();

        expectedFile.setLastModified(time);
        assertTrue(expectedFile.exists(), "file not found: " + expectedFile);
        assertEquals("org/sample/company/test.jsp", FileUtils.fileRead(expectedFile), "file incorrect");

        // housekeeping
        expectedFile.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB")
    @Test
    public void testExplodedWarWithEJB(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        project.addArtifact(ejbArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithJar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithJar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithJar")
    @Test
    public void testExplodedWarWithJar(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), new DefaultArtifactHandler("jar"));
        project.addArtifact(jarArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/lib/jarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithEJB")
    @Test
    public void testExplodedWarWithEJBClient(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        EJBClientArtifactStub ejbArtifact = new EJBClientArtifactStub(getBasedir());
        project.addArtifact(ejbArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbclientartifact-0.0-Test-client.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithTLD-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithTLD-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithTLD")
    @Test
    public void testExplodedWarWithTLD(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        TLDArtifactStub tldArtifact = new TLDArtifactStub(getBasedir());
        project.addArtifact(tldArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedTLDArtifact = new File(webAppDirectory, "WEB-INF/tld/tldartifact-0.0-Test.tld");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedTLDArtifact.exists(), "tld artifact not found: " + expectedTLDArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedTLDArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithPAR-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithPAR-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithPAR")
    @Test
    public void testExplodedWarWithPAR(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        PARArtifactStub parartifact = new PARArtifactStub(getBasedir());
        project.addArtifact(parartifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedPARArtifact = new File(webAppDirectory, "WEB-INF/lib/parartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedPARArtifact.exists(), "par artifact not found: " + expectedPARArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedPARArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithAar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithAar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithAar")
    @Test
    public void testExplodedWarWithAar(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        ArtifactStub aarArtifact = new AarArtifactStub(getBasedir(), new DefaultArtifactHandler("jar"));
        project.addArtifact(aarArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/services/aarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithMar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithMar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithMar")
    @Test
    public void testExplodedWarWithMar(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        ArtifactStub marArtifact = new MarArtifactStub(getBasedir(), new DefaultArtifactHandler("jar"));
        project.addArtifact(marArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/modules/marartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithXar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithXar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithXar")
    @Test
    public void testExplodedWarWithXar(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        ArtifactStub xarArtifact = new XarArtifactStub(getBasedir(), new DefaultArtifactHandler("jar"));
        project.addArtifact(xarArtifact);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/extensions/xarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithDuplicateDependencies-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithDuplicateDependencies-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithDuplicateDependencies")
    @Test
    public void testExplodedWarWithDuplicateDependencies(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithDuplicateDependencies";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        // configure mojo
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        ejbArtifact.setGroupId("org.sample.ejb");
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub(getBasedir());
        ejbArtifactDup.setGroupId("org.dup.ejb");
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact-0.0-Test.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact);
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarDuplicateWithClassifier-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarDuplicateWithClassifier-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarDuplicateWithClassifier")
    @Test
    public void testExplodedWarDuplicateWithClassifier(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        ejbArtifact.setGroupId("org.sample.ejb");
        EJBArtifactStubWithClassifier ejbArtifactDup = new EJBArtifactStubWithClassifier(getBasedir());
        ejbArtifactDup.setGroupId("org.sample.ejb");
        ejbArtifactDup.setClassifier("classifier");
        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test-classifier.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact);
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithClasses-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithClasses-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithClasses")
    @Test
    public void testExplodedWarWithClasses(WarExplodedMojo mojo) throws Exception {
        // configure mojo
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppDirectory = mojo.getWebappDirectory();
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedClass = new File(webAppDirectory, "WEB-INF/classes/sample-servlet.clazz");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedClass.exists(), "classes not found: " + expectedClass);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedClass.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(name = "warSourceIncludes", value = "**/*sit.jsp")
    @MojoParameter(name = "warSourceExcludes", value = "**/last*.*")
    @Test
    public void testExplodedWarWithSourceIncludeExclude(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithSourceIncludeExclude";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertFalse(expectedWebSource2File.exists(), "source files found: " + expectedWebSource2File);
        assertTrue(expectedWEBXMLDir.exists(), "WEB XML not found: " + expectedWEBXMLDir);
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLDir.delete();
        expectedMETAINFDir.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @MojoParameter(name = "dependentWarIncludes", value = "**/*Include.jsp,**/*.xml")
    @MojoParameter(name = "dependentWarExcludes", value = "**/*Exclude*,**/MANIFEST.MF")
    @MojoParameter(
            name = "workDirectory",
            value =
                    "target/test-classes/unit/warexplodedmojo/test-dir/war/work-ExplodedWarWithWarDependencyIncludeExclude")
    @Test
    public void testExplodedWarWithWarDependencyIncludeExclude(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithWarDependencyIncludeExclude";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);

        IncludeExcludeWarArtifactStub includeexcludeWarArtifact = new IncludeExcludeWarArtifactStub(getBasedir());
        // configure mojo
        configureMojo(mojo, includeexcludeWarArtifact, classesDir, webAppSource, webAppDirectory);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedManifestFile = new File(webAppDirectory, "META-INF/MANIFEST.MF");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedIncludedWARFile = new File(webAppDirectory, "/org/sample/company/testInclude.jsp");
        File expectedExcludedWarfile = new File(webAppDirectory, "/org/sample/companyExclude/test.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        // check include-exclude.war in the unit test dir under resources to verify the list of files
        assertTrue(expectedWEBXMLFile.exists(), "web xml not found: " + expectedWEBXMLFile);
        assertFalse(expectedManifestFile.exists(), "manifest file found: " + expectedManifestFile);
        assertTrue(expectedIncludedWARFile.exists(), "war file not found: " + expectedIncludedWARFile);
        assertFalse(expectedExcludedWarfile.exists(), "war file not found: " + expectedExcludedWarfile);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedManifestFile.delete();
        expectedWEBXMLFile.delete();
        expectedIncludedWARFile.delete();
        expectedExcludedWarfile.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @Test
    public void testExplodedWarWithSourceModificationCheck(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithSourceModificationCheck";
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory);

        // destination file is already created manually containing an "error" string
        // source is newer than the destination file
        mojo.execute();

        // validate operation

        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // 1st phase destination is older than source
        // destination starts with a value of error replaced with a blank source
        assertNotEquals(
                "error",
                FileUtils.fileRead(expectedWebSourceFile),
                "source files not updated with new copy: " + expectedWebSourceFile);

        // housekeeping
        expectedWEBINFDir.delete();
        expectedMETAINFDir.delete();
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @Test
    @Disabled // TODO interpolation of extension does not work
    public void testExplodedWarWithOutputFileNameMapping(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithFileNameMapping";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), artifactHandler);

        // configure mojo
        mojo.setOutputFileNameMapping("@{artifactId}@.@{extension}@");
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(jarArtifact);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/lib/jarartifact.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @Test
    public void testExplodedWarWithOutputFileNameMappingAndDuplicateDependencies(WarExplodedMojo mojo)
            throws Exception {
        // setup test data
        String testId = "ExplodedWarWithFileNameMappingAndDuplicateDependencies";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        ejbArtifact.setGroupId("org.sample.ejb");
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub(getBasedir());
        ejbArtifactDup.setGroupId("org.dup.ejb");

        // configure mojo
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);
        mojo.setOutputFileNameMapping("@{artifactId}@.@{extension}@");
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact);
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact);

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    private void configureMojo(WarExplodedMojo mojo, File classesDir, File webAppSource, File webAppDirectory)
            throws Exception {
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
    }

    private void configureMojo(
            WarExplodedMojo mojo, ArtifactStub artifact, File classesDir, File webAppSource, File webAppDirectory)
            throws Exception {
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        project.addArtifact(artifact);
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
    }

    private void configureMojo(
            WarExplodedMojo mojo, File classesDir, File webAppSource, File webAppDirectory, MavenProject project) {
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
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
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir);
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
}
