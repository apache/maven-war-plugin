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
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
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
import org.apache.maven.plugins.war.stub.WarOverlayStub;
import org.apache.maven.plugins.war.stub.XarArtifactStub;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystemSession;

import static org.junit.Assert.assertNotEquals;

public class WarExplodedMojoTest extends AbstractMojoTestCase {

    protected static final File OVERLAYS_TEMP_DIR = new File(getBasedir(), "target/test-overlays/");
    protected static final File OVERLAYS_ROOT_DIR = new File(getBasedir(), "target/test-classes/overlays/");
    protected static final String MANIFEST_PATH = "META-INF" + File.separator + "MANIFEST.MF";
    protected WarExplodedMojo mojo;

    protected File getPomFile() {
        return new File(getBasedir(), "/target/test-classes/unit/warexplodedmojo/plugin-config.xml");
    }

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir");
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testSimpleExplodedWar() throws Exception {
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

        assertTrue("sampeResource not found", sampleResource.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("resources doesn't exist: " + expectedWebResourceFile, expectedWebResourceFile.exists());
        assertTrue("WEB-INF not found", expectedWEBINFDir.exists());
        assertTrue("META-INF not found", expectedMETAINFDir.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testSimpleExplodedWarWTargetPath() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("resources doesn't exist: " + expectedWebResourceFile, expectedWebResourceFile.exists());
        assertTrue("WEB-INF not found", expectedWEBINFDir.exists());
        assertTrue("META-INF not found", expectedMETAINFDir.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWebResourceFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithCustomWebXML() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("WEB XML not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists());
        assertTrue("META-INF not found", expectedMETAINFDir.exists());
        assertEquals("WEB XML not correct", mojo.getWebXml().toString(), FileUtils.fileRead(expectedWEBXMLFile));

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLFile.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithContainerConfigXML() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("WEB-INF not found", expectedWEBINFDir.exists());
        assertTrue(
                "Container Config XML not found:" + expectedContainerConfigXMLFile.toString(),
                expectedContainerConfigXMLFile.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedContainerConfigXMLFile.delete();
        expectedWEBINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithSimpleExternalWARFile() throws Exception {
        // setup test data
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());

        String testId = "ExplodedWarWithSimpleExternalWARFile";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        File simpleWarFile = warArtifact.getFile();

        assertTrue("simple war not found: " + simpleWarFile.toString(), simpleWarFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        // check simple.war in the unit test dir under resources to verify the list of files
        assertTrue("web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists());
        assertTrue("war file not found: " + expectedWARFile.toString(), expectedWARFile.exists());

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
    public void testExplodedWarMergeWarLocalFileOverride() throws Exception {
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

        assertTrue("file not found: " + expectedFile.toString(), expectedFile.exists());
        assertEquals("file incorrect", simpleJSP.toString(), FileUtils.fileRead(expectedFile));

        // check when the merged war file is newer - so set an old time on the local file
        long time =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2005-1-1").getTime();
        simpleJSP.setLastModified(time);
        expectedFile.setLastModified(time);

        project.addArtifact(warArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "workDirectory", workDirectory);
        mojo.execute();

        assertTrue("file not found: " + expectedFile.toString(), expectedFile.exists());
        assertEquals("file incorrect", simpleJSP.toString(), FileUtils.fileRead(expectedFile));

        // housekeeping
        expectedFile.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithEJB() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBArtifactStub ejbArtifact = new EJBArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        assertTrue("ejb jar not found: " + ejbFile.toString(), ejbFile.exists());

        // configure mojo
        project.addArtifact(ejbArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    public void testExplodedWarWithJar() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithJar";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), artifactHandler);
        File jarFile = jarArtifact.getFile();

        assertTrue("jar not found: " + jarFile.toString(), jarFile.exists());

        // configure mojo
        project.addArtifact(jarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/lib/jarartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithEJBClient() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithEJB";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        EJBClientArtifactStub ejbArtifact = new EJBClientArtifactStub(getBasedir());
        File ejbFile = ejbArtifact.getFile();

        assertTrue("ejb jar not found: " + ejbFile.toString(), ejbFile.exists());

        // configure mojo
        project.addArtifact(ejbArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedEJBArtifact = new File(webAppDirectory, "WEB-INF/lib/ejbclientartifact-0.0-Test-client.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithTLD() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithTLD";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        TLDArtifactStub tldArtifact = new TLDArtifactStub(getBasedir());
        File tldFile = tldArtifact.getFile();

        assertTrue("tld jar not found: " + tldFile.getAbsolutePath(), tldFile.exists());

        // configure mojo
        project.addArtifact(tldArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedTLDArtifact = new File(webAppDirectory, "WEB-INF/tld/tldartifact-0.0-Test.tld");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("tld artifact not found: " + expectedTLDArtifact.toString(), expectedTLDArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedTLDArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithPAR() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithPAR";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        PARArtifactStub parartifact = new PARArtifactStub(getBasedir());
        File parFile = parartifact.getFile();

        assertTrue("par not found: " + parFile.getAbsolutePath(), parFile.exists());

        // configure mojo
        project.addArtifact(parartifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedPARArtifact = new File(webAppDirectory, "WEB-INF/lib/parartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("par artifact not found: " + expectedPARArtifact.toString(), expectedPARArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedPARArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithAar() throws Exception {
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

        assertTrue("jar not found: " + aarFile.toString(), aarFile.exists());

        // configure mojo
        project.addArtifact(aarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/services/aarartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithMar() throws Exception {
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

        assertTrue("jar not found: " + marFile.toString(), marFile.exists());

        // configure mojo
        project.addArtifact(marArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/modules/marartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithXar() throws Exception {
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

        assertTrue("jar not found: " + xarFile.toString(), xarFile.exists());

        // configure mojo
        project.addArtifact(xarArtifact);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        // final name form is <artifactId>-<version>.<type>
        File expectedJarArtifact = new File(webAppDirectory, "WEB-INF/extensions/xarartifact-0.0-Test.jar");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithDuplicateDependencies() throws Exception {
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
        assertTrue("ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists());
        assertTrue("ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarDuplicateWithClassifier() throws Exception {
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
        assertTrue("ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists());
        assertTrue("ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithClasses() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("classes not found: " + expectedClass.toString(), expectedClass.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedClass.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithSourceIncludeExclude() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertFalse("source files found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("WEB XML not found: " + expectedWEBXMLDir.toString(), expectedWEBXMLDir.exists());
        assertTrue("META-INF not found", expectedMETAINFDir.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedWEBXMLDir.delete();
        expectedMETAINFDir.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithWarDependencyIncludeExclude() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithWarDependencyIncludeExclude";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        IncludeExcludeWarArtifactStub includeexcludeWarArtifact = new IncludeExcludeWarArtifactStub(getBasedir());
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        File includeExcludeWarFile = includeexcludeWarArtifact.getFile();

        assertTrue("war not found: " + includeExcludeWarFile.toString(), includeExcludeWarFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        // check include-exclude.war in the unit test dir under resources to verify the list of files
        assertTrue("web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists());
        assertFalse("manifest file found: " + expectedManifestFile.toString(), expectedManifestFile.exists());
        assertTrue("war file not found: " + expectedIncludedWARFile.toString(), expectedIncludedWARFile.exists());
        assertFalse("war file not found: " + expectedExcludedWarfile.toString(), expectedExcludedWarfile.exists());

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
    public void testExplodedWarWithSourceModificationCheck() throws Exception {
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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("WEB-INF not found", expectedWEBINFDir.exists());
        assertTrue("META-INF not found", expectedMETAINFDir.exists());

        // 1st phase destination is older than source
        // destination starts with a value of error replaced with a blank source
        assertNotEquals(
                "source files not updated with new copy: " + expectedWebSourceFile.toString(),
                "error",
                FileUtils.fileRead(expectedWebSourceFile));

        // housekeeping
        expectedWEBINFDir.delete();
        expectedMETAINFDir.delete();
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithOutputFileNameMapping() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithFileNameMapping";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        ArtifactHandler artifactHandler = (ArtifactHandler) lookup(ArtifactHandler.ROLE, "jar");
        ArtifactStub jarArtifact = new JarArtifactStub(getBasedir(), artifactHandler);
        File jarFile = jarArtifact.getFile();

        assertTrue("jar not found: " + jarFile.toString(), jarFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("jar artifact not found: " + expectedJarArtifact.toString(), expectedJarArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedJarArtifact.delete();
    }

    /**
     * @throws Exception in case of an error.
     */
    public void testExplodedWarWithOutputFileNameMappingAndDuplicateDependencies() throws Exception {
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
        assertTrue("ejb not found: " + ejbFile.getAbsolutePath(), ejbFile.exists());

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

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("ejb artifact not found: " + expectedEJBArtifact.toString(), expectedEJBArtifact.exists());
        assertTrue("ejb dup artifact not found: " + expectedEJBDupArtifact.toString(), expectedEJBDupArtifact.exists());

        // housekeeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedEJBArtifact.delete();
        expectedEJBDupArtifact.delete();
    }

    public void setUp() throws Exception {
        super.setUp();

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
                .setSystemProperties(System.getProperties())
                .setStartTime(new Date());

        MavenSession mavenSession =
                new MavenSession((PlexusContainer) null, (RepositorySystemSession) null, request, null);
        getContainer().addComponent(mavenSession, MavenSession.class.getName());
        mojo = (WarExplodedMojo) lookupMojo("exploded", getPomFile());
    }

    /**
     * Configures the exploded mojo for the specified test.
     *
     * If the {@code sourceFiles} parameter is {@code null}, sample JSPs are created by default.
     *
     * @param testId the id of the test
     * @param artifactStubs the dependencies (may be null)
     * @param sourceFiles the source files to create (may be null)
     * @return the webapp directory
     * @throws Exception if an error occurs while configuring the mojo
     */
    protected File setUpMojo(final String testId, ArtifactStub[] artifactStubs, String[] sourceFiles) throws Exception {
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final File webAppDirectory = new File(getTestDirectory(), testId);

        // Create the webapp sources
        File webAppSource;
        if (sourceFiles == null) {
            webAppSource = createWebAppSource(testId);
        } else {
            webAppSource = createWebAppSource(testId, false);
            for (String sourceFile : sourceFiles) {
                File sample = new File(webAppSource, sourceFile);
                createFile(sample);
            }
        }

        final File classesDir = createClassesDir(testId, true);
        final File workDirectory = new File(getTestDirectory(), "/war/work-" + testId);
        createDir(workDirectory);

        if (artifactStubs != null) {
            for (ArtifactStub artifactStub : artifactStubs) {
                project.addArtifact(artifactStub);
            }
        }

        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "workDirectory", workDirectory);

        return webAppDirectory;
    }

    /**
     * Configures the exploded mojo for the specified test.
     *
     * @param testId the id of the test
     * @param artifactStubs the dependencies (may be null)
     * @return the webapp directory
     * @throws Exception if an error occurs while configuring the mojo
     */
    protected File setUpMojo(final String testId, ArtifactStub[] artifactStubs) throws Exception {
        return setUpMojo(testId, artifactStubs, null);
    }

    /**
     * Cleans up a directory.
     *
     * @param directory the directory to remove
     * @throws IOException if an error occurred while removing the directory
     */
    protected void cleanDirectory(File directory) throws IOException {
        if (directory != null && directory.isDirectory() && directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    /**
     * Asserts the default content of the war based on the specified webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @return a list of File objects that have been asserted
     */
    protected List<File> assertDefaultContent(File webAppDirectory) {
        // Validate content of the webapp
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");

        assertTrue("source file not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source file not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());

        final List<File> content = new ArrayList<>();
        content.add(expectedWebSourceFile);
        content.add(expectedWebSource2File);

        return content;
    }

    /**
     * Asserts the web.xml file of the war based on the specified webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @return a list with the web.xml File object
     */
    protected List<File> assertWebXml(File webAppDirectory) {
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        assertTrue("web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists());

        final List<File> content = new ArrayList<>();
        content.add(expectedWEBXMLFile);

        return content;
    }

    /**
     * Asserts custom content of the war based on the specified webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @param filePaths an array of file paths relative to the webapp directory
     * @param customMessage a custom message if an assertion fails
     * @return a list of File objects that have been inspected
     */
    protected List<File> assertCustomContent(File webAppDirectory, String[] filePaths, String customMessage) {
        final List<File> content = new ArrayList<>();
        for (String filePath : filePaths) {
            final File expectedFile = new File(webAppDirectory, filePath);
            if (customMessage != null) {
                assertTrue(customMessage + " - " + expectedFile.toString(), expectedFile.exists());
            } else {
                assertTrue("source file not found: " + expectedFile.toString(), expectedFile.exists());
            }
            content.add(expectedFile);
        }
        return content;
    }

    /**
     * Asserts that the webapp contains only the specified files.
     *
     * @param webAppDirectory the webapp directory
     * @param expectedFiles the expected files
     * @param filter an optional filter to ignore some resources
     */
    protected void assertWebAppContent(File webAppDirectory, List<File> expectedFiles, FileFilter filter) {
        final List<File> webAppContent = new ArrayList<>();
        if (filter != null) {
            buildFilesList(webAppDirectory, filter, webAppContent);
        } else {
            buildFilesList(webAppDirectory, new FileFilterImpl(webAppDirectory, null), webAppContent);
        }

        // Now we have the files, sort them.
        Collections.sort(expectedFiles);
        Collections.sort(webAppContent);
        assertEquals(
                "Invalid webapp content, expected " + expectedFiles.size() + "file(s) " + expectedFiles + " but got "
                        + webAppContent.size() + " file(s) " + webAppContent,
                expectedFiles,
                webAppContent);
    }

    /**
     * Builds the list of files and directories from the specified dir.
     *
     * Note that the filter is not used the usual way. If the filter does not accept the current file, it's not added
     * but yet the subdirectories are added if any.
     *
     * @param dir the base directory
     * @param filter the filter
     * @param content the current content, updated recursively
     */
    private void buildFilesList(final File dir, FileFilter filter, final List<File> content) {
        final File[] files = dir.listFiles();

        for (File file : files) {
            // Add the file if the filter is ok with it
            if (filter.accept(file)) {
                content.add(file);
            }

            // Even if the file is not accepted and is a directory, add it
            if (file.isDirectory()) {
                buildFilesList(file, filter, content);
            }
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
    protected void configureMojo(
            AbstractWarMojo mojo, File classesDir, File webAppSource, File webAppDir, MavenProjectBasicStub project)
            throws Exception {
        setVariableValueToObject(mojo, "outdatedCheckPath", "WEB-INF/lib/");
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
            assertTrue("can not create test dir: " + dir.toString(), dir.mkdirs());
        }
    }

    protected void createFile(File testFile, String body) throws Exception {
        createDir(testFile.getParentFile());
        FileUtils.fileWrite(testFile.toString(), body);

        assertTrue("could not create file: " + testFile, testFile.exists());
    }

    protected void createFile(File testFile) throws Exception {
        createFile(testFile, testFile.toString());
    }

    /**
     * Generates test war.
     * Generates war with such a structure:
     * <ul>
     * <li>jsp
     * <ul>
     * <li>d
     * <ul>
     * <li>a.jsp</li>
     * <li>b.jsp</li>
     * <li>c.jsp</li>
     * </ul>
     * </li>
     * <li>a.jsp</li>
     * <li>b.jsp</li>
     * <li>c.jsp</li>
     * </ul>
     * </li>
     * <li>WEB-INF
     * <ul>
     * <li>classes
     * <ul>
     * <li>a.clazz</li>
     * <li>b.clazz</li>
     * <li>c.clazz</li>
     * </ul>
     * </li>
     * <li>lib
     * <ul>
     * <li>a.jar</li>
     * <li>b.jar</li>
     * <li>c.jar</li>
     * </ul>
     * </li>
     * <li>web.xml</li>
     * </ul>
     * </li>
     * </ul>
     * Each of the files will contain: id+'-'+path
     *
     * @param id the id of the overlay containing the full structure
     * @return the war file
     * @throws Exception if an error occurs
     */
    protected File generateFullOverlayWar(String id) throws Exception {
        final File destFile = new File(OVERLAYS_TEMP_DIR, id + ".war");
        if (destFile.exists()) {
            return destFile;
        }

        // Archive was not yet created for that id so let's create it
        final File rootDir = new File(OVERLAYS_ROOT_DIR, id);
        rootDir.mkdirs();
        String[] filePaths = new String[] {
            "jsp/d/a.jsp",
            "jsp/d/b.jsp",
            "jsp/d/c.jsp",
            "jsp/a.jsp",
            "jsp/b.jsp",
            "jsp/c.jsp",
            "WEB-INF/classes/a.clazz",
            "WEB-INF/classes/b.clazz",
            "WEB-INF/classes/c.clazz",
            "WEB-INF/lib/a.jar",
            "WEB-INF/lib/b.jar",
            "WEB-INF/lib/c.jar",
            "WEB-INF/web.xml"
        };

        for (String filePath : filePaths) {
            createFile(new File(rootDir, filePath), id + "-" + filePath);
        }

        createArchive(rootDir, destFile);
        return destFile;
    }

    /**
     * Builds a test overlay.
     *
     * @param id the id of the overlay (see test/resources/overlays)
     * @return a test war artifact with the content of the given test overlay
     */
    protected ArtifactStub buildWarOverlayStub(String id) {
        // Create war file
        final File destFile = new File(OVERLAYS_TEMP_DIR, id + ".war");
        if (!destFile.exists()) {
            createArchive(new File(OVERLAYS_ROOT_DIR, id), destFile);
        }

        return new WarOverlayStub(getBasedir(), id, destFile);
    }

    protected File getOverlayFile(String id, String filePath) {
        final File overlayDir = new File(OVERLAYS_ROOT_DIR, id);
        final File file = new File(overlayDir, filePath);

        // Make sure the file exists
        assertTrue(
                "Overlay file " + filePath + " does not exist for overlay " + id + " at " + file.getAbsolutePath(),
                file.exists());
        return file;
    }

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

    class FileFilterImpl implements FileFilter {

        private final List<String> rejectedFilePaths;

        private final int webAppDirIndex;

        FileFilterImpl(File webAppDirectory, String[] rejectedFilePaths) {
            if (rejectedFilePaths != null) {
                this.rejectedFilePaths = Arrays.asList(rejectedFilePaths);
            } else {
                this.rejectedFilePaths = new ArrayList<>();
            }
            this.webAppDirIndex = webAppDirectory.getAbsolutePath().length() + 1;
        }

        public boolean accept(File file) {
            String effectiveRelativePath = buildRelativePath(file);
            return !(rejectedFilePaths.contains(effectiveRelativePath) || file.isDirectory());
        }

        private String buildRelativePath(File f) {
            return f.getAbsolutePath().substring(webAppDirIndex);
        }
    }
}
