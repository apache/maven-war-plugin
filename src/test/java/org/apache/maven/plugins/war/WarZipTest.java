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
import java.net.URI;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugins.war.overlay.DefaultOverlay;
import org.apache.maven.plugins.war.stub.MavenZipProject;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.apache.maven.plugins.war.stub.ZipArtifactStub;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 * @since 7 Oct 07
 */
@MojoTest
public class WarZipTest {
    @Inject
    private ArtifactHandler artifactHandler;

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(name = "classesDirectory", value = "target/test-classes/unit/warziptest/one-zip-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = "target/test-classes/unit/warziptest/one-zip-test-data/source/")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warziptest/one-zip-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warziptest/one-zip")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warziptest/one-zip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithNoSkip(WarMojo mojo) throws Exception {
        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setType("zip");
        mojo.addOverlay(overlay);

        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenZipProject project = new MavenZipProject();
        project.setArtifact(warArtifact);
        project.getArtifacts().add(buildZipArtifact());
        mojo.setProject(project);

        mojo.execute();

        File webAppDirectory = mojo.getWebappDirectory();
        File foo = new File(webAppDirectory, "foo.txt");
        assertTrue(foo.exists(), "foo.txt not exists");
        assertTrue(foo.isFile(), "foo.txt not a file");

        File barDirectory = new File(webAppDirectory, "bar");
        assertTrue(barDirectory.exists(), "bar directory not exists");
        assertTrue(barDirectory.isDirectory(), "bar not a directory");

        File bar = new File(barDirectory, "bar.txt");
        assertTrue(bar.exists(), "bar/bar.txt not exists");
        assertTrue(bar.isFile(), "bar/bar.txt not a file");
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(name = "classesDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/source/")
    @MojoParameter(name = "webXml", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithTargetPathOverlay(WarMojo mojo) throws Exception {
        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setSkip(false);
        overlay.setType("zip");
        overlay.setTargetPath("overridePath");
        mojo.addOverlay(overlay);

        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenZipProject project = new MavenZipProject();
        project.setArtifact(warArtifact);
        project.getArtifacts().add(buildZipArtifact());
        mojo.setProject(project);

        mojo.execute();

        File webAppDirectory = mojo.getWebappDirectory();
        File foo = new File(webAppDirectory.getPath() + File.separatorChar + "overridePath", "foo.txt");
        assertTrue(foo.exists(), "foo.txt not exists");
        assertTrue(foo.isFile(), "foo.txt not a file");

        File barDirectory = new File(webAppDirectory.getPath() + File.separatorChar + "overridePath", "bar");
        assertTrue(barDirectory.exists(), "bar directory not exists");
        assertTrue(barDirectory.isDirectory(), "bar not a directory");

        File bar = new File(barDirectory, "bar.txt");
        assertTrue(bar.exists(), "bar/bar.txt not exists");
        assertTrue(bar.isFile(), "bar/bar.txt not a file");
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-default-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipDefaultSkip(WarMojo mojo) throws Exception {
        String testId = "one-zip-overlay-default-skip";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, xmlSource);

        mojo.execute();

        assertZipContentNotHere(webAppDirectory);
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-force-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithForceSkip(WarMojo mojo) throws Exception {
        String testId = "one-zip-overlay-force-skip";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});

        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, xmlSource);

        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setSkip(true);
        overlay.setType("zip");
        mojo.addOverlay(overlay);

        mojo.execute();

        assertZipContentNotHere(webAppDirectory);
    }

    private void configureMojo(
            WarMojo mojo, File classesDir, File webAppSource, File webAppDirectory, File xmlSource, Overlay overlay)
            throws Exception {
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, xmlSource);
        mojo.addOverlay(overlay);
    }

    private void configureMojo(WarMojo mojo, File classesDir, File webAppSource, File webAppDirectory, File xmlSource)
            throws Exception {
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenZipProject project = new MavenZipProject();
        project.setArtifact(warArtifact);
        project.getArtifacts().add(buildZipArtifact());
        mojo.setProject(project);
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setWebXml(new File(xmlSource, "web.xml"));
    }

    private Artifact buildZipArtifact() throws Exception {
        File zipFile = new File(getTestDirectory(), "foobar.zip");
        return new ZipArtifactStub("src/test/resources/unit/warziptest", artifactHandler, zipFile);
    }

    private File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warziptest");
    }

    private void assertZipContentNotHere(File webAppDirectory) {
        File foo = new File(webAppDirectory.getPath() + File.separatorChar + "overridePath", "foo.txt");
        assertFalse(foo.exists(), "foo.txt exists");
        assertFalse(foo.isFile(), "foo.txt a file");

        File barDirectory = new File(webAppDirectory.getPath() + File.separatorChar + "overridePath", "bar");
        assertFalse(barDirectory.exists(), "bar directory exists");
        assertFalse(barDirectory.isDirectory(), "bar is a directory");

        File bar = new File(barDirectory, "bar.txt");
        assertFalse(bar.exists(), "bar/bar.txt exists");
        assertFalse(bar.isFile(), "bar/bar.txt is a file");
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
