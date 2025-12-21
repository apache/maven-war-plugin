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
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.maven.artifact.handler.ArtifactHandler;
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
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarExplodedMojoTest extends AbstractWarExplodedMojoTest {

    @Override
    protected File getPomFile() {
        return new File(getBasedir(), "/target/test-classes/unit/warexplodedmojo/plugin-config.xml");
    }

    @Override
    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir");
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void simpleExplodedWar() throws Exception {
        // setup test data
        String testId = "SimpleExplodedWar";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppResource = new File(getTestDirectory(), testId + "-resources");
        File webAppDirectory = new File(getTestDirectory(), testId);
        File sampleResource = new File(webAppResource, "pix/panis_na.jpg");
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};

        createFile(sampleResource);

        assertTrue(sampleResource.exists(), "sampeResource not found");

        // configure mojo
        resources[0].setDirectory(webAppResource.getAbsolutePath());
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "webResources", resources);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWebResourceFile = new File(webAppDirectory, "pix/panis_na.jpg");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedWebResourceFile.exists(), "resources doesn't exist: " + expectedWebResourceFile);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void simpleExplodedWarWTargetPath() throws Exception {
        // setup test data
        String testId = "SimpleExplodedWar";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppResource = new File(getTestDirectory(), "resources");
        File webAppDirectory = new File(getTestDirectory(), testId);
        File sampleResource = new File(webAppResource, "pix/panis_na.jpg");
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};

        createFile(sampleResource);

        // configure mojo
        resources[0].setDirectory(webAppResource.getAbsolutePath());
        resources[0].setTargetPath("targetPath");
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "webResources", resources);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWebResourceFile = new File(webAppDirectory, "targetPath/pix/panis_na.jpg");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedWebResourceFile.exists(), "resources doesn't exist: " + expectedWebResourceFile);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithCustomWebXML() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithCustomWebXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedWEBXMLFile.exists(), "WEB XML not found: " + expectedWEBXMLFile.toString());
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");
        assertEquals(mojo.getWebXml().toString(), FileUtils.fileRead(expectedWEBXMLFile), "WEB XML not correct");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithContainerConfigXML() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithContainerConfigXML";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File classesDir = createClassesDir(testId, true);
        File webAppSource = createWebAppSource(testId);
        File xmlSource = createXMLConfigDir(testId, new String[] {"config.xml"});
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.setContainerConfigXML(new File(xmlSource, "config.xml"));
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedContainerConfigXMLFile = new File(webAppDirectory, "META-INF/config.xml");
        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(
                expectedContainerConfigXMLFile.exists(),
                "Container Config XML not found:" + expectedContainerConfigXMLFile.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedContainerConfigXMLFile.delete();
        expectedWEBINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithSimpleExternalWARFile() throws Exception {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());

        String testId = "ExplodedWarWithSimpleExternalWARFile";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        File simpleWarFile = warArtifact.getFile();

        assertTrue(simpleWarFile.exists(), "simple war not found: " + simpleWarFile.toString());

        createDir(workDirectory);

        // configure mojo
        project.addArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "workDirectory", workDirectory);
        mojo.execute();

        // validate operation - META-INF is automatically excluded so remove the file from the list
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedWARFile = new File(webAppDirectory, "/org/sample/company/test.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        // check simple.war in the unit test dir under resources to verify the list of files
        assertTrue(expectedWEBXMLFile.exists(), "web xml not found: " + expectedWEBXMLFile.toString());
        assertTrue(expectedWARFile.exists(), "war file not found: " + expectedWARFile.toString());

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
    @Test
    void explodedWarMergeWarLocalFileOverride() throws Exception {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());

        String testId = "testExplodedWarMergeWarLocalFileOverride";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = getWebAppSource(testId);
        File simpleJSP = new File(webAppSource, "org/sample/company/test.jsp");
        createFile(simpleJSP);

        File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        createDir(workDirectory);

        File classesDir = createClassesDir(testId, true);

        // configure mojo
        project.addArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "workDirectory", workDirectory);
        mojo.execute();

        // validate operation
        File expectedFile = new File(webAppDirectory, "/org/sample/company/test.jsp");

        assertTrue(expectedFile.exists(), "file not found: " + expectedFile.toString());
        assertEquals(simpleJSP.toString(), FileUtils.fileRead(expectedFile), "file incorrect");

        // check when the merged war file is newer - so set an old time on the local file
        long time =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2005-1-1").getTime();
        simpleJSP.setLastModified(time);
        expectedFile.setLastModified(time);

        project.addArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "workDirectory", workDirectory);
        mojo.execute();

        assertTrue(expectedFile.exists(), "file not found: " + expectedFile.toString());
        assertEquals(simpleJSP.toString(), FileUtils.fileRead(expectedFile), "file incorrect");

        // housekeeping
        expectedFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithEJB() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        assertTrue(ejbFile.exists(), "ejb jar not found: " + ejbFile.toString());

        // configure mojo
        project.addArtifact(ejbArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    @Test
    void explodedWarWithJar() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithJar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), artifactHandler);
        File jarFile = jarArtifact.getFile();

        assertTrue(jarFile.exists(), "jar not found: " + jarFile.toString());

        // configure mojo
        project.addArtifact(jarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/lib/jarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithEJBClient() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBClientArtifactStub ejbArtifact = new EJBClientArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        assertTrue(ejbFile.exists(), "ejb jar not found: " + ejbFile.toString());

        // configure mojo
        project.addArtifact(ejbArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbclientartifact-0.0-Test-client.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithTLD() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithTLD";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        TLDArtifactStub tldArtifact = new TLDArtifactStub(getBasedir());
        File tldFile = tldArtifact.getFile();

        assertTrue(tldFile.exists(), "tld jar not found: " + tldFile.getAbsolutePath());

        // configure mojo
        project.addArtifact(tldArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedTLDArtifact = new File(webAppDirectory, "WEB-INF/tld/tldartifact-0.0-Test.tld");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedTLDArtifact.exists(), "tld artifact not found: " + expectedTLDArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedTLDArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithPAR() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithPAR";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        PARArtifactStub parartifact = new PARArtifactStub(getBasedir());
        File parFile = parartifact.getFile();

        assertTrue(parFile.exists(), "par not found: " + parFile.getAbsolutePath());

        // configure mojo
        project.addArtifact(parartifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedPARArtifact = new File(webAppDirectory, "WEB-INF/lib/parartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedPARArtifact.exists(), "par artifact not found: " + expectedPARArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedPARArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithAar() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithAar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        // Fake here since the aar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub aarArtifact = new AarArtifactStub(getBasedir(), artifactHandler);
        File aarFile = aarArtifact.getFile();

        assertTrue(aarFile.exists(), "jar not found: " + aarFile.toString());

        // configure mojo
        project.addArtifact(aarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/services/aarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithMar() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithMar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        // Fake here since the mar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub marArtifact = new MarArtifactStub(getBasedir(), artifactHandler);
        File marFile = marArtifact.getFile();

        assertTrue(marFile.exists(), "jar not found: " + marFile.toString());

        // configure mojo
        project.addArtifact(marArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/modules/marartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithXar() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithXar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        // Fake here since the xar artifact handler does not exist: no biggie
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub xarArtifact = new XarArtifactStub(getBasedir(), artifactHandler);
        File xarFile = xarArtifact.getFile();

        assertTrue(xarFile.exists(), "jar not found: " + xarFile.toString());

        // configure mojo
        project.addArtifact(xarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/extensions/xarartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithDuplicateDependencies() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithDuplicateDependencies";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue(ejbFile.exists(), "ejb not found: " + ejbFile.getAbsolutePath());

        // configure mojo
        ejbArtifact.setGroupId("org.sample.ejb");
        ejbArtifactDup.setGroupId("org.dup.ejb");
        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact-0.0-Test.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact-0.0-Test.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact.toString());
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarDuplicateWithClassifier() throws Exception {
        // setup test data
        String testId = "ExplodedWarDuplicateWithClassifier";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        EJBArtifactStubWithClassifier ejbArtifactDup = new EJBArtifactStubWithClassifier(getBasedir());

        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue(ejbFile.exists(), "ejb not found: " + ejbFile.getAbsolutePath());

        // configure mojo

        ejbArtifact.setGroupId("org.sample.ejb");
        ejbArtifactDup.setGroupId("org.sample.ejb");

        ejbArtifactDup.setClassifier("classifier");

        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);

        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test-classifier.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact.toString());
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithClasses() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithClasses";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);

        // configure mojo
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedClass = new File(webAppDirectory, "WEB-INF/classes/sample-servlet.clazz");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedClass.exists(), "classes not found: " + expectedClass.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedClass.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithSourceIncludeExclude() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithSourceIncludeExclude";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "warSourceIncludes", "**/*sit.jsp");
        setVariableValueToObject(mojo, "warSourceExcludes", "**/last*.*");
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedWEBXMLDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertFalse(expectedWebSource2File.exists(), "source files found: " + expectedWebSource2File.toString());
        assertTrue(expectedWEBXMLDir.exists(), "WEB XML not found: " + expectedWEBXMLDir.toString());
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLDir.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithWarDependencyIncludeExclude() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithWarDependencyIncludeExclude";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        IncludeExcludeWarArtifactStub includeexcludeWarArtifact = new IncludeExcludeWarArtifactStub(getBasedir());
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        File includeExcludeWarFile = includeexcludeWarArtifact.getFile();

        assertTrue(includeExcludeWarFile.exists(), "war not found: " + includeExcludeWarFile.toString());

        createDir(workDirectory);

        // configure mojo
        project.addArtifact(includeexcludeWarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "dependentWarIncludes", "**/*Include.jsp,**/*.xml");
        setVariableValueToObject(mojo, "dependentWarExcludes", "**/*Exclude*,**/MANIFEST.MF");
        setVariableValueToObject(mojo, "workDirectory", workDirectory);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedManifestFile = new File(webAppDirectory, "META-INF/MANIFEST.MF");
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        File expectedIncludedWARFile = new File(webAppDirectory, "/org/sample/company/testInclude.jsp");
        File expectedExcludedWarfile = new File(webAppDirectory, "/org/sample/companyExclude/test.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        // check include-exclude.war in the unit test dir under resources to verify the list of files
        assertTrue(expectedWEBXMLFile.exists(), "web xml not found: " + expectedWEBXMLFile.toString());
        assertFalse(expectedManifestFile.exists(), "manifest file found: " + expectedManifestFile.toString());
        assertTrue(expectedIncludedWARFile.exists(), "war file not found: " + expectedIncludedWARFile.toString());
        assertFalse(expectedExcludedWarfile.exists(), "war file not found: " + expectedExcludedWarfile.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedManifestFile.delete();
        expectedWEBXMLFile.delete();
        expectedIncludedWARFile.delete();
        expectedExcludedWarfile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithSourceModificationCheck() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithSourceModificationCheck";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppDirectory = new File(getTestDirectory(), testId);

        // configure mojo
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);

        // destination file is already created manually containing an "error" string
        // source is newer than the destination file
        mojo.execute();

        // validate operation

        File expectedWEBINFDir = new File(webAppDirectory, "WEB-INF");
        File expectedMETAINFDir = new File(webAppDirectory, "META-INF");
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");

        // 1st phase destination is older than source
        // destination starts with a value of error replaced with a blank source
        assertNotEquals(
                "error",
                FileUtils.fileRead(expectedWebSourceFile),
                "source files not updated with new copy: " + expectedWebSourceFile.toString());

        // housekeeping
        expectedWEBINFDir.delete();
        expectedMETAINFDir.delete();
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithOutputFileNameMapping() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithFileNameMapping";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), artifactHandler);
        File jarFile = jarArtifact.getFile();

        assertTrue(jarFile.exists(), "jar not found: " + jarFile.toString());

        // configure mojo
        project.addArtifact(jarArtifact);
        mojo.setOutputFileNameMapping("@{artifactId}@.@{extension}@");
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/lib/jarartifact.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedJarArtifact.exists(), "jar artifact not found: " + expectedJarArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    @Test
    void explodedWarWithOutputFileNameMappingAndDuplicateDependencies() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithFileNameMappingAndDuplicateDependencies";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        EJBArtifactStub ejbArtifactDup = new EJBArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        // ejbArtifact has a hard coded file, only one assert is needed
        assertTrue(ejbFile.exists(), "ejb not found: " + ejbFile.getAbsolutePath());

        // configure mojo
        ejbArtifact.setGroupId("org.sample.ejb");
        ejbArtifactDup.setGroupId("org.dup.ejb");
        project.addArtifact(ejbArtifact);
        project.addArtifact(ejbArtifactDup);
        mojo.setOutputFileNameMapping("@{artifactId}@.@{extension}@");
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/org.sample.ejb-ejbartifact.jar");
        File expectedEJBDupArtifact = new File(webAppDirectory, "WEB-INF/lib/org.dup.ejb-ejbartifact.jar");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedEJBArtifact.exists(), "ejb artifact not found: " + expectedEJBArtifact.toString());
        assertTrue(expectedEJBDupArtifact.exists(), "ejb dup artifact not found: " + expectedEJBDupArtifact.toString());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }
}
