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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.overlay.DefaultOverlay;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.WarOverlayStub;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.getVariableValueFromObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Stephane Nicoll
 */
@MojoTest
public class WarOverlaysTest {

    private static final File OVERLAYS_TEMP_DIR =
            new File(getBasedir(), "target/test-classes/unit/waroverlays/test-overlays/");
    private static final File OVERLAYS_ROOT_DIR = new File(getBasedir(), "target/test-classes/overlays/");
    private static final String MANIFEST_PATH = "META-INF" + File.separator + "MANIFEST.MF";

    @BeforeEach
    public void setUp() throws Exception {
        //        generateFullOverlayWar("overlay-full-1");
        //        generateFullOverlayWar("overlay-full-2");
        //        generateFullOverlayWar("overlay-full-3");
    }

    private File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/waroverlays");
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/waroverlays/war/work-no-overlay")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/no-overlay-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/no-overlay-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/waroverlays/no-overlay")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/waroverlays/no-overlay-test-data/xml-config/web.xml")
    @Test
    public void testNoOverlay(WarExplodedMojo mojo) throws Exception {
        mojo.setProject(new MavenProjectArtifactsStub());
        mojo.execute();

        // Validate content of the webapp
        assertDefaultContent((File) getVariableValueFromObject(mojo, "webappDirectory"));
        assertWebXml((File) getVariableValueFromObject(mojo, "webappDirectory"));
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/waroverlays/war/work-default-overlay")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/default-overlay-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/default-overlay-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/waroverlays/default-overlay")
    @Test
    public void testDefaultOverlay(WarExplodedMojo mojo) throws Exception {
        // Create war file
        final File destFile = new File(OVERLAYS_TEMP_DIR, "overlay-one" + ".war");
        if (!destFile.exists()) {
            createArchive(new File(OVERLAYS_ROOT_DIR, "overlay-one"), destFile);
        }

        final ArtifactStub overlay = new WarOverlayStub(MojoExtension.getBasedir(), "overlay-one", destFile);
        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay);

        mojo.setProject(project);

        mojo.execute();

        final List<File> assertedFiles = new ArrayList<>();
        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        assertedFiles.addAll(assertDefaultContent(webAppDirectory));
        assertedFiles.addAll(assertWebXml(webAppDirectory));
        assertedFiles.addAll(assertCustomContent(
                webAppDirectory, new String[] {"index.jsp", "login.jsp"}, "overlay file not found"));

        // index and login come from overlay1
        assertOverlayedFile(webAppDirectory, "overlay-one", "index.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-one", "login.jsp");

        // Ok now check that there is no more files/directories
        final FileFilter filter = new FileFilterImpl(webAppDirectory, new String[] {MANIFEST_PATH});
        assertWebAppContent(webAppDirectory, assertedFiles, filter);
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/waroverlays/war/work-default-overlays")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/default-overlays-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/default-overlays-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/waroverlays/default-overlays")
    @Test
    public void testDefaultOverlays(WarExplodedMojo mojo) throws Exception {
        final ArtifactStub overlay = buildWarOverlayStub("overlay-one");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-two");
        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay, overlay2);

        mojo.setProject(project);

        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        final List<File> assertedFiles = new ArrayList<>();
        assertedFiles.addAll(assertDefaultContent(webAppDirectory));
        assertedFiles.addAll(assertWebXml(webAppDirectory));
        assertedFiles.addAll(assertCustomContent(
                webAppDirectory, new String[] {"index.jsp", "login.jsp", "admin.jsp"}, "overlay file not found"));

        // index and login come from overlay1
        assertOverlayedFile(webAppDirectory, "overlay-one", "index.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-one", "login.jsp");

        // admin comes from overlay2
        // index and login comes from overlay1
        assertOverlayedFile(webAppDirectory, "overlay-two", "admin.jsp");

        // Ok now check that there is no more files/directories
        final FileFilter filter = new FileFilterImpl(webAppDirectory, new String[] {MANIFEST_PATH});
        assertWebAppContent(webAppDirectory, assertedFiles, filter);
    }

    /**
     * Merge a dependent WAR when a file in the war source directory overrides one found in the WAR.
     *
     * It also tests completeness of the resulting war as well as the proper order of dependencies.
     *
     * @throws Exception if any error occurs
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/waroverlays/war/work-scenario-one-default-settings")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-default-settings-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-default-settings-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-default-settings")
    @Test
    public void testScenarioOneWithDefaulSettings(WarExplodedMojo mojo) throws Exception {
        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub("overlay-full-1");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-full-2");
        final ArtifactStub overlay3 = buildWarOverlayStub("overlay-full-3");

        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay1, overlay2, overlay3);

        mojo.setProject(project);

        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        assertScenariOne("scenario-one-default-settings", webAppDirectory);
    }

    /**
     * Tests that specifying the overlay explicitely has the same behavior as the default (i.e. order, etc).
     *
     * The default project is not specified in this case so it is processed first by default
     *
     * @throws Exception if an error occurs
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/waroverlays/war/work-scenario-one-overlay-settings")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-overlay-settings-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-overlay-settings-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-overlay-settings")
    @Test
    public void testScenarioOneWithOverlaySettings(WarExplodedMojo mojo) throws Exception {
        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub("overlay-full-1");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-full-2");
        final ArtifactStub overlay3 = buildWarOverlayStub("overlay-full-3");

        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay1, overlay2, overlay3);
        mojo.setProject(project);

        // Add the tags
        final List<Overlay> overlays = new ArrayList<>();
        overlays.add(new DefaultOverlay(overlay1));
        overlays.add(new DefaultOverlay(overlay2));
        overlays.add(new DefaultOverlay(overlay3));
        mojo.setOverlays(overlays);

        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        assertScenariOne("scenario-one-overlay-settings", webAppDirectory);
    }

    /**
     * Tests that specifying the overlay explicitely has the same behavior as the default (i.e. order, etc).
     *
     * The default project is explicitely specified so this should match the default.
     *
     * @throws Exception if an error occurs
     */
    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/waroverlays/war/work-scenario-one-full-settings")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-full-settings-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-full-settings-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/waroverlays/scenario-one-full-settings")
    @Test
    public void testScenarioOneWithFullSettings(WarExplodedMojo mojo) throws Exception {
        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub("overlay-full-1");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-full-2");
        final ArtifactStub overlay3 = buildWarOverlayStub("overlay-full-3");

        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay1, overlay2, overlay3);
        mojo.setProject(project);

        // Add the tags
        final List<Overlay> overlays = new ArrayList<>();
        // Add the default project explicitely
        overlays.add(mojo.getCurrentProjectOverlay());
        // Other overlays
        overlays.add(new DefaultOverlay(overlay1));
        overlays.add(new DefaultOverlay(overlay2));
        overlays.add(new DefaultOverlay(overlay3));
        mojo.setOverlays(overlays);

        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        assertScenariOne("scenario-one-full-settings", webAppDirectory);
    }

    /**
     * Runs the mojo and asserts a scenerio with 3 overlays and no includes/excludes settings.
     *
     * @param testId thie id of the test
     * @param webAppDirectory the webapp directory
     * @throws Exception if an exception occurs
     */
    private void assertScenariOne(String testId, File webAppDirectory) throws Exception {
        final List<File> assertedFiles = new ArrayList<>();
        assertedFiles.addAll(assertWebXml(webAppDirectory));
        assertedFiles.addAll(assertCustomContent(
                webAppDirectory,
                new String[] {
                    "jsp/a.jsp",
                    "jsp/b.jsp",
                    "jsp/c.jsp",
                    "jsp/d/a.jsp",
                    "jsp/d/b.jsp",
                    "jsp/d/c.jsp",
                    "org/sample/company/test.jsp",
                    "WEB-INF/classes/a.clazz",
                    "WEB-INF/classes/b.clazz",
                    "WEB-INF/classes/c.clazz",
                    "WEB-INF/lib/a.jar",
                    "WEB-INF/lib/b.jar",
                    "WEB-INF/lib/c.jar"
                },
                "overlay file not found"));

        // Those files should come from the source webapp without any config
        assertDefaultFileContent(testId, webAppDirectory, "jsp/b.jsp");
        assertDefaultFileContent(testId, webAppDirectory, "org/sample/company/test.jsp");

        // Everything else comes from overlay1 (order of addition in the dependencies)
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/a.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/c.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/d/a.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/d/b.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/d/c.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/web.xml");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/classes/a.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/classes/b.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/classes/c.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/lib/a.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/lib/b.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "WEB-INF/lib/c.jar");

        // Ok now check that there is no more files/directories
        final FileFilter filter = new FileFilterImpl(webAppDirectory, new String[] {MANIFEST_PATH});
        assertWebAppContent(webAppDirectory, assertedFiles, filter);
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/waroverlays/war/work-overlays-includes-excludes-multiple-defs")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-includes-excludes-multiple-defs-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-includes-excludes-multiple-defs-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-includes-excludes-multiple-defs")
    @Test
    public void testOverlaysIncludesExcludesWithMultipleDefinitions(WarExplodedMojo mojo) throws Exception {
        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub("overlay-full-1");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-full-2");
        final ArtifactStub overlay3 = buildWarOverlayStub("overlay-full-3");

        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay1, overlay2, overlay3);
        mojo.setProject(project);


        Overlay over1 = new DefaultOverlay(overlay3);
        over1.setExcludes("**/a.*,**/c.*,**/*.xml");

        Overlay over2 = new DefaultOverlay(overlay1);
        over2.setIncludes("jsp/d/*");
        over2.setExcludes("jsp/d/a.jsp");

        Overlay over3 = new DefaultOverlay(overlay3);
        over3.setIncludes("**/*.jsp");

        Overlay over4 = new DefaultOverlay(overlay2);

        mojo.setOverlays(new LinkedList<>());
        mojo.addOverlay(over1);
        mojo.addOverlay(over2);
        mojo.addOverlay(over3);
        mojo.addOverlay(mojo.getCurrentProjectOverlay());
        mojo.addOverlay(over4);


        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        final List<File> assertedFiles = new ArrayList<>();
        assertedFiles.addAll(assertWebXml(webAppDirectory));
        assertedFiles.addAll(assertCustomContent(
                webAppDirectory,
                new String[] {
                    "jsp/a.jsp",
                    "jsp/b.jsp",
                    "jsp/c.jsp",
                    "jsp/d/a.jsp",
                    "jsp/d/b.jsp",
                    "jsp/d/c.jsp",
                    "org/sample/company/test.jsp",
                    "WEB-INF/classes/a.clazz",
                    "WEB-INF/classes/b.clazz",
                    "WEB-INF/classes/c.clazz",
                    "WEB-INF/lib/a.jar",
                    "WEB-INF/lib/b.jar",
                    "WEB-INF/lib/c.jar"
                },
                "overlay file not found"));

        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/a.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/b.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/c.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/d/a.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/d/b.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/d/c.jsp");
        assertDefaultFileContent("overlays-includes-excludes-multiple-defs", webAppDirectory, "org/sample/company/test.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/web.xml");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/classes/a.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "WEB-INF/classes/b.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/classes/c.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/lib/a.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "WEB-INF/lib/b.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/lib/c.jar");

        // Ok now check that there is no more files/directories
        final FileFilter filter = new FileFilterImpl(webAppDirectory, new String[] {MANIFEST_PATH});
        assertWebAppContent(webAppDirectory, assertedFiles, filter);
    }

    @InjectMojo(goal = "exploded", pom = "src/test/resources/unit/waroverlays/default.xml")
    @MojoParameter(
            name = "workDirectory",
            value = "target/test-classes/unit/waroverlays/war/work-overlays-includes-excludes-multiple-defs2")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-overlays-includes-excludes-multiple-defs2-test-data/classes")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-overlays-includes-excludes-multiple-defs2-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/waroverlays/overlays-includes-excludes-multiple-defs2")
    @Test
    public void testOverlaysIncludesExcludesWithMultipleDefinitions2(WarExplodedMojo mojo) throws Exception {
        // Add an overlay
        final ArtifactStub overlay1 = buildWarOverlayStub("overlay-full-1");
        final ArtifactStub overlay2 = buildWarOverlayStub("overlay-full-2");
        final ArtifactStub overlay3 = buildWarOverlayStub("overlay-full-3");

        final MavenProjectArtifactsStub project = createProjectWithOverlays(overlay1, overlay2, overlay3);
        mojo.setProject(project);

        Overlay over1 = new DefaultOverlay(overlay3);
        over1.setExcludes("**/a.*,**/c.*,**/*.xml,jsp/b.jsp");

        Overlay over2 = new DefaultOverlay(overlay1);
        over2.setIncludes("jsp/d/*");
        over2.setExcludes("jsp/d/a.jsp");

        Overlay over3 = new DefaultOverlay(overlay3);
        over3.setIncludes("**/*.jsp");
        over3.setExcludes("jsp/b.jsp");

        Overlay over4 = new DefaultOverlay(overlay2);

        mojo.setOverlays(new LinkedList<>());
        mojo.addOverlay(over1);
        mojo.addOverlay(over2);
        mojo.addOverlay(over3);
        mojo.addOverlay(mojo.getCurrentProjectOverlay());
        mojo.addOverlay(over4);

        mojo.execute();

        File webAppDirectory = (File) getVariableValueFromObject(mojo, "webappDirectory");
        final List<File> assertedFiles = new ArrayList<>();
        assertedFiles.addAll(assertWebXml(webAppDirectory));
        assertedFiles.addAll(assertCustomContent(
                webAppDirectory,
                new String[] {
                    "jsp/a.jsp",
                    "jsp/b.jsp",
                    "jsp/c.jsp",
                    "jsp/d/a.jsp",
                    "jsp/d/b.jsp",
                    "jsp/d/c.jsp",
                    "org/sample/company/test.jsp",
                    "WEB-INF/classes/a.clazz",
                    "WEB-INF/classes/b.clazz",
                    "WEB-INF/classes/c.clazz",
                    "WEB-INF/lib/a.jar",
                    "WEB-INF/lib/b.jar",
                    "WEB-INF/lib/c.jar"
                },
                "overlay file not found"));

        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/a.jsp");
        assertDefaultFileContent("overlays-includes-excludes-multiple-defs2", webAppDirectory, "jsp/b.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/c.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/d/a.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "jsp/d/b.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-1", "jsp/d/c.jsp");
        assertDefaultFileContent("overlays-includes-excludes-multiple-defs2", webAppDirectory, "org/sample/company/test.jsp");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/web.xml");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/classes/a.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "WEB-INF/classes/b.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/classes/c.clazz");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/lib/a.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-3", "WEB-INF/lib/b.jar");
        assertOverlayedFile(webAppDirectory, "overlay-full-2", "WEB-INF/lib/c.jar");

        // Ok now check that there is no more files/directories
        final FileFilter filter = new FileFilterImpl(webAppDirectory, new String[] {MANIFEST_PATH});
        assertWebAppContent(webAppDirectory, assertedFiles, filter);
    }

    // Helpers

    private MavenProjectArtifactsStub createProjectWithOverlays(ArtifactStub... artifactStubs) throws Exception {
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        for (ArtifactStub artifactStub : artifactStubs) {
            project.addArtifact(artifactStub);
        }
        return project;
    }

    /**
     * Asserts that the content of an overlayed file is correct.
     *
     * Note that the {@code filePath} is relative to both the webapp directory and the overlayed directory, defined by
     * the {@code overlayId}.
     *
     * @param webAppDirectory the webapp directory
     * @param overlayId the id of the overlay
     * @param filePath the relative path
     * @throws IOException if an error occurred while reading the files
     */
    private void assertOverlayedFile(File webAppDirectory, String overlayId, String filePath) throws IOException {
        final File webAppFile = new File(webAppDirectory, filePath);
        final File overlayFile = getOverlayFile(overlayId, filePath);
        assertEquals(
                FileUtils.fileRead(overlayFile),
                FileUtils.fileRead(webAppFile),
                "Wrong content for overlayed file " + filePath);
    }

    /**
     * Asserts that the content of an overlaid file is correct.
     *
     * Note that the {@code filePath} is relative to both the webapp directory and the overlayed directory, defined by
     * the {@code overlayId}.
     *
     * @param testId te id of the test
     * @param webAppDirectory the webapp directory
     * @param filePath the relative path
     * @throws IOException if an error occurred while reading the files
     */
    private void assertDefaultFileContent(String testId, File webAppDirectory, String filePath) throws Exception {
        final File webAppFile = new File(webAppDirectory, filePath);
        final File sourceFile = new File(getWebAppSource(testId), filePath);
        final String expectedContent = sourceFile.toString();
        assertEquals(expectedContent, FileUtils.fileRead(webAppFile), "Wrong content for file " + filePath);
    }

    /**
     * Asserts the default content of the war based on the specified webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @return a list of File objects that have been asserted
     */
    private List<File> assertDefaultContent(File webAppDirectory) {
        // Validate content of the webapp
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");

        assertTrue(expectedWebSourceFile.exists(), "source file not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source file not found: " + expectedWebSource2File);

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
    private List<File> assertWebXml(File webAppDirectory) {
        File expectedWEBXMLFile = new File(webAppDirectory, "WEB-INF/web.xml");
        assertTrue(expectedWEBXMLFile.exists(), "web xml not found: " + expectedWEBXMLFile);

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
    private List<File> assertCustomContent(File webAppDirectory, String[] filePaths, String customMessage) {
        final List<File> content = new ArrayList<>();
        for (String filePath : filePaths) {
            final File expectedFile = new File(webAppDirectory, filePath);
            if (customMessage != null) {
                assertTrue(expectedFile.exists(), customMessage + " - " + expectedFile);
            } else {
                assertTrue(expectedFile.exists(), "source file not found: " + expectedFile);
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
    private void assertWebAppContent(File webAppDirectory, List<File> expectedFiles, FileFilter filter) {
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
                expectedFiles,
                webAppContent,
                "Invalid webapp content, expected " + expectedFiles.size() + "file(s) " + expectedFiles + " but got "
                        + webAppContent.size() + " file(s) " + webAppContent);
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
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir);
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
    private File generateFullOverlayWar(String id) throws Exception {
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
    private ArtifactStub buildWarOverlayStub(String id) {
        // Create war file
        final File destFile = new File(OVERLAYS_TEMP_DIR, id + ".war");
        if (!destFile.exists()) {
            createArchive(new File(OVERLAYS_ROOT_DIR, id), destFile);
        }

        return new WarOverlayStub(getBasedir(), id, destFile);
    }

    private File getOverlayFile(String id, String filePath) {
        final File overlayDir = new File(OVERLAYS_ROOT_DIR, id);
        final File file = new File(overlayDir, filePath);

        // Make sure the file exists
        assertTrue(
                file.exists(),
                "Overlay file " + filePath + " does not exist for overlay " + id + " at " + file.getAbsolutePath());
        return file;
    }

    private void createArchive(final File directory, final File destinationFile) {
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
