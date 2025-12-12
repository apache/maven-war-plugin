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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugins.war.overlay.DefaultOverlay;
import org.apache.maven.plugins.war.stub.MavenZipProject;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.apache.maven.plugins.war.stub.ZipArtifactStub;
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
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-targetPath-output")
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
            name = "classesDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-skip")
    @MojoParameter(name = "outputDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipDefaultSkip(WarMojo mojo) throws Exception {
        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenZipProject project = new MavenZipProject();
        project.setArtifact(warArtifact);
        project.getArtifacts().add(buildZipArtifact());
        mojo.setProject(project);

        mojo.execute();

        assertZipContentNotHere(mojo.getWebappDirectory());
    }

    @InjectMojo(goal = "war", pom = "src/test/resources/unit/warziptest/war-with-zip.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/source/")
    @MojoParameter(
            name = "webXml",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-skip-test-data/xml-config/web.xml")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warziptest/one-zip-overlay-skip")
    @MojoParameter(
            name = "outputDirectory",
            value = "target/test-classes/unit/warziptest/one-zip-overlay-force-skip-output")
    @MojoParameter(name = "warName", value = "simple")
    @MojoParameter(name = "workDirectory", value = "target/test-classes/unit/warziptest/work")
    @Test
    public void testOneZipWithForceSkip(WarMojo mojo) throws Exception {

        WarArtifactStub warArtifact = new WarArtifactStub(getBasedir());
        MavenZipProject project = new MavenZipProject();
        project.setArtifact(warArtifact);
        project.getArtifacts().add(buildZipArtifact());
        mojo.setProject(project);

        Overlay overlay = new DefaultOverlay(buildZipArtifact());
        overlay.setSkip(true);
        overlay.setType("zip");
        mojo.addOverlay(overlay);

        mojo.execute();

        assertZipContentNotHere(mojo.getWebappDirectory());
    }

    private Artifact buildZipArtifact() throws Exception {
        File zipFile =
                new File(new File(MojoExtension.getBasedir(), "target/test-classes/unit/warziptest"), "foobar.zip");
        return new ZipArtifactStub("src/test/resources/unit/warziptest", artifactHandler, zipFile);
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
}
