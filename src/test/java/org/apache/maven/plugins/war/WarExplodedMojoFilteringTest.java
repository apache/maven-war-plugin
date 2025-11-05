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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.apache.maven.plugins.war.stub.WarOverlayStub;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystemSession;

/**
 * @author Olivier Lamy
 * @since 21 juil. 2008
 */
public class WarExplodedMojoFilteringTest extends AbstractMojoTestCase {

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
    @SuppressWarnings("checkstyle:MethodLength")
    public void testExplodedWarWithResourceFiltering() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithResourceFiltering";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppResource = new File(getTestDirectory(), testId + "-test-data/resources");
        File sampleResource = new File(webAppResource, "custom-setting.cfg");
        File sampleResourceWDir = new File(webAppResource, "custom-config/custom-setting.cfg");
        List<String> filterList = new LinkedList<>();
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};

        createFile(sampleResource);
        createFile(sampleResourceWDir);

        String ls = System.getProperty("line.separator");
        final String comment = "# this is comment created by author@somewhere@";
        // prepare web resources
        String content = comment + ls;
        content += "system_key_1=${user.dir}" + ls;
        content += "system_key_2=@user.dir@" + ls;
        content += "project_key_1=${is_this_simple}" + ls;
        content += "project_key_2=@is_this_simple@" + ls;
        content += "project_name_1=${project.name}" + ls;
        content += "project_name_2=@project.name@" + ls;
        content += "system_property_1=${system.property}" + ls;
        content += "system_property_2=@system.property@" + ls;
        FileUtils.fileWrite(sampleResourceWDir.getAbsolutePath(), content);
        FileUtils.fileWrite(sampleResource.getAbsolutePath(), content);

        lookup(MavenSession.class).getSystemProperties().setProperty("system.property", "system-property-value");

        // configure mojo
        project.addProperty("is_this_simple", "i_think_so");
        resources[0].setDirectory(webAppResource.getAbsolutePath());
        resources[0].setFiltering(true);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "webResources", resources);
        setVariableValueToObject(mojo, "filters", filterList);

        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedResourceFile = new File(webAppDirectory, "custom-setting.cfg");
        File expectedResourceWDirFile = new File(webAppDirectory, "custom-config/custom-setting.cfg");

        assertTrue("source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists());
        assertTrue("source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists());
        assertTrue("resource file not found:" + expectedResourceFile.toString(), expectedResourceFile.exists());
        assertTrue(
                "resource file with dir not found:" + expectedResourceWDirFile.toString(),
                expectedResourceWDirFile.exists());

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        BufferedReader reader = new BufferedReader(new StringReader(content));

        assertEquals("error in filtering using System Properties", comment, reader.readLine());

        String line = reader.readLine();
        assertEquals(
                "error in filtering using System properties", "system_key_1=" + System.getProperty("user.dir"), line);
        line = reader.readLine();
        assertEquals(
                "error in filtering using System properties", "system_key_2=" + System.getProperty("user.dir"), line);

        assertEquals("error in filtering using project properties", "project_key_1=i_think_so", reader.readLine());
        assertEquals("error in filtering using project properties", "project_key_2=i_think_so", reader.readLine());

        assertEquals("error in filtering using project properties", "project_name_1=Test Project ", reader.readLine());
        assertEquals("error in filtering using project properties", "project_name_2=Test Project ", reader.readLine());

        assertEquals(
                "error in filtering using System properties",
                "system_property_1=system-property-value",
                reader.readLine());
        assertEquals(
                "error in filtering using System properties",
                "system_property_2=system-property-value",
                reader.readLine());

        // update property, and generate again
        lookup(MavenSession.class).getSystemProperties().setProperty("system.property", "new-system-property-value");
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        reader = new BufferedReader(new StringReader(content));

        assertEquals("error in filtering using System Properties", comment, reader.readLine());

        assertEquals(
                "error in filtering using System properties",
                "system_key_1=" + System.getProperty("user.dir"),
                reader.readLine());
        assertEquals(
                "error in filtering using System properties",
                "system_key_2=" + System.getProperty("user.dir"),
                reader.readLine());

        assertEquals("error in filtering using project properties", "project_key_1=i_think_so", reader.readLine());
        assertEquals("error in filtering using project properties", "project_key_2=i_think_so", reader.readLine());

        assertEquals("error in filtering using project properties", "project_name_1=Test Project ", reader.readLine());
        assertEquals("error in filtering using project properties", "project_name_2=Test Project ", reader.readLine());

        assertEquals(
                "error in filtering using System properties",
                "system_property_1=new-system-property-value",
                reader.readLine());
        assertEquals(
                "error in filtering using System properties",
                "system_property_2=new-system-property-value",
                reader.readLine());

        // update property, and generate again
        File filterFile = new File(getTestDirectory(), testId + "-test-data/filters/filter.properties");
        createFile(filterFile);
        filterList.add(filterFile.getAbsolutePath());
        content += "resource_key_1=${resource_value_1}\n";
        content += "resource_key_2=@resource_value_2@\n" + content;
        FileUtils.fileWrite(sampleResourceWDir.getAbsolutePath(), content);
        FileUtils.fileWrite(sampleResource.getAbsolutePath(), content);
        String filterContent = "resource_value_1=this_is_filtered\n";
        filterContent += "resource_value_2=this_is_filtered";
        FileUtils.fileWrite(filterFile.getAbsolutePath(), filterContent);

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        reader = new BufferedReader(new StringReader(content));

        assertEquals("error in filtering using System Properties", comment, reader.readLine());

        assertEquals(
                "error in filtering using System properties",
                "system_key_1=" + System.getProperty("user.dir"),
                reader.readLine());
        assertEquals(
                "error in filtering using System properties",
                "system_key_2=" + System.getProperty("user.dir"),
                reader.readLine());

        assertEquals("error in filtering using project properties", "project_key_1=i_think_so", reader.readLine());
        assertEquals("error in filtering using project properties", "project_key_2=i_think_so", reader.readLine());

        assertEquals("error in filtering using project properties", "project_name_1=Test Project ", reader.readLine());
        assertEquals("error in filtering using project properties", "project_name_2=Test Project ", reader.readLine());

        assertEquals(
                "error in filtering using System properties",
                "system_property_1=new-system-property-value",
                reader.readLine());
        assertEquals(
                "error in filtering using System properties",
                "system_property_2=new-system-property-value",
                reader.readLine());

        assertEquals("error in filtering using filter files", "resource_key_1=this_is_filtered", reader.readLine());
        assertEquals("error in filtering using filter files", "resource_key_2=this_is_filtered", reader.readLine());

        // house-keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedResourceFile.delete();
        expectedResourceWDirFile.delete();
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
