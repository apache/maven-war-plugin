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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.overlay.DefaultOverlay;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.MavenZipProject;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.apache.maven.plugins.war.stub.WarOverlayStub;
import org.apache.maven.plugins.war.stub.ZipArtifactStub;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Olivier Lamy
 * @since 7 Oct 07
 */
@MojoTest
public class WarZipTest {
    @Inject
    private ArtifactHandler artifactHandler;
    protected static final File OVERLAYS_TEMP_DIR = new File(getBasedir(), "target/test-overlays/");
    protected static final File OVERLAYS_ROOT_DIR = new File(getBasedir(), "target/test-classes/overlays/");

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warziptest");
    }

//    @BeforeEach
//    public void setUp() throws Exception {
//        super.setUp();
//
//        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
//                .setSystemProperties(System.getProperties())
//                .setStartTime(new Date());
//
//        MavenSession mavenSession =
//                new MavenSession((PlexusContainer) null, (RepositorySystemSession) null, request, null);
//        getContainer().addComponent(mavenSession, MavenSession.class.getName());
//        mojo = (WarMojo) lookupMojo("war", pomFile);
//    }

    private Artifact buildZipArtifact() throws Exception {
        File zipFile = new File(getTestDirectory(), "foobar.zip");
        return new ZipArtifactStub("src/test/resources/unit/warziptest", artifactHandler, zipFile);
    }

    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warmojotest/SimpleWarPackagingExcludeWithIncludesRegEx-output")
    @MojoParameter(name = "warName", value = "simple")
    private File configureMojo(String testId) throws Exception {
        MavenZipProject project = new MavenZipProject();
        String outputDir = getTestDirectory().getAbsolutePath() + File.separatorChar + testId + "-output";
        // clean up
        File outputDirFile = new File(outputDir);
        if (outputDirFile.exists()) {
            FileUtils.deleteDirectory(outputDirFile);
        }
        File webAppDirectory = new File(getTestDirectory(), testId);
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, true);
        File xmlSource = createXMLConfigDir(testId, new String[] {"web.xml"});
        project.setArtifact(warArtifact);

//        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
//        setVariableValueToObject(mojo, "workDirectory", new File(getTestDirectory(), "work"));
//        mojo.setWebXml(new File(xmlSource, "web.xml"));

        project.getArtifacts().add(buildZipArtifact());

        return webAppDirectory;
    }

    @InjectMojo(goal="war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithNoSkip(WarMojo mojo) throws Exception {
        MavenZipProject project = new MavenZipProject();
        File webAppDirectory1 = new File(getTestDirectory(), "one-zip");
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        File webAppSource = createWebAppSource("one-zip");
        File classesDir = createClassesDir("one-zip", true);
        File xmlSource = createXMLConfigDir("one-zip", new String[] {"web.xml"});
        project.setArtifact(warArtifact);

        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory1);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        project.getArtifacts().add(buildZipArtifact());

        File webAppDirectory = webAppDirectory1;

        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        // overlay.setSkip( false );
        overlay.setType("zip");
        mojo.addOverlay(overlay);
        mojo.execute();

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

    @InjectMojo(goal="war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithTargetPathOverlay(WarMojo mojo) throws Exception {
        MavenZipProject project = new MavenZipProject();
        File webAppDirectory = new File(getTestDirectory(), "one-zip-overlay-targetPath");
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        File webAppSource = createWebAppSource("one-zip-overlay-targetPath");
        File classesDir = createClassesDir("one-zip-overlay-targetPath", true);
        File xmlSource = createXMLConfigDir("one-zip-overlay-targetPath", new String[] {"web.xml"});
        project.setArtifact(warArtifact);

        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        project.getArtifacts().add(buildZipArtifact());

        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setSkip(false);
        overlay.setType("zip");
        overlay.setTargetPath("overridePath");
        mojo.addOverlay(overlay);

        mojo.execute();

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

    @InjectMojo(goal="war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipDefaultSkip(WarMojo mojo) throws Exception {
        MavenZipProject project = new MavenZipProject();
        File webAppDirectory = new File(getTestDirectory(), "one-zip-overlay-skip");
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        File webAppSource = createWebAppSource("one-zip-overlay-skip");
        File classesDir = createClassesDir("one-zip-overlay-skip", true);
        File xmlSource = createXMLConfigDir("one-zip-overlay-skip", new String[] {"web.xml"});
        project.setArtifact(warArtifact);

        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        project.getArtifacts().add(buildZipArtifact());

        mojo.execute();

        assertZipContentNotHere(webAppDirectory);
    }

    @InjectMojo(goal="war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithForceSkip(WarMojo mojo) throws Exception {
        MavenZipProject project = new MavenZipProject();
        String outputDir = getTestDirectory().getAbsolutePath() + File.separatorChar + "one-zip-overlay-skip" + "-output";
        // clean up
        File outputDirFile = new File(outputDir);
        if (outputDirFile.exists()) {
            FileUtils.deleteDirectory(outputDirFile);
        }
        File webAppDirectory = new File(getTestDirectory(), "one-zip-overlay-skip");
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        File webAppSource = createWebAppSource("one-zip-overlay-skip");
        File classesDir = createClassesDir("one-zip-overlay-skip", true);
        File xmlSource = createXMLConfigDir("one-zip-overlay-skip", new String[] {"web.xml"});
        project.setArtifact(warArtifact);

        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
        mojo.setWebXml(new File(xmlSource, "web.xml"));

        project.getArtifacts().add(buildZipArtifact());

        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setSkip(true);
        overlay.setType("zip");
        mojo.addOverlay(overlay);

        mojo.execute();
        assertZipContentNotHere(webAppDirectory);
    }

    protected void assertZipContentNotHere(File webAppDirectory) {
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
                file.exists(),
                "Overlay file " + filePath + " does not exist for overlay " + id + " at " + file.getAbsolutePath());
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
}
