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
package org.apache.maven.plugins.war.overlay;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugins.war.Overlay;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.WarArtifactStub;
import org.junit.jupiter.api.Test;

import static org.apache.maven.plugins.war.Overlay.DEFAULT_EXCLUDES;
import static org.apache.maven.plugins.war.Overlay.DEFAULT_INCLUDES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Stephane Nicoll
 */
class OverlayManagerTest {

    @Test
    public void testEmptyProject() throws Exception {
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final List<Overlay> overlays = new ArrayList<>();
        final Overlay currentProjectOverlay = Overlay.createInstance();
        OverlayManager manager =
                new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay);
        assertNotNull(manager.getOverlays());
        assertEquals(1, manager.getOverlays().size());
        assertEquals(currentProjectOverlay, manager.getOverlays().get(0));
    }

    // TODO investigate what test should do ....
    void testAutodetectSimpleOverlay(Overlay currentProjectOverlay) throws Exception {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact("test", "test-webapp");
        project.addArtifact(first);

        final List<Overlay> overlays = new ArrayList<>();

        OverlayManager manager =
                new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay);
        assertNotNull(manager.getOverlays());
        assertEquals(2, manager.getOverlays().size());
        assertEquals(currentProjectOverlay, manager.getOverlays().get(0));
        assertEquals(new DefaultOverlay(first), manager.getOverlays().get(1));
    }

    @Test
    void testSimpleOverlay() throws Exception {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact("test", "test-webapp");
        project.addArtifact(first);

        final List<Overlay> overlays = new ArrayList<>();
        overlays.add(new DefaultOverlay(first));

        final Overlay currentProjectOverlay = Overlay.createInstance();
        OverlayManager manager =
                new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay);
        assertNotNull(manager.getOverlays());
        assertEquals(2, manager.getOverlays().size());
        assertEquals(Overlay.createInstance(), manager.getOverlays().get(0));
        assertEquals(overlays.get(0), manager.getOverlays().get(1));
    }

    @Test
    void testUnknownOverlay() throws Exception {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact("test", "test-webapp");
        project.addArtifact(first);

        final List<Overlay> overlays = new ArrayList<>();
        overlays.add(new Overlay("test", "test-webapp-2"));

        final Overlay currentProjectOverlay = Overlay.createInstance();
        assertThrows(
                InvalidOverlayConfigurationException.class,
                () -> new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay));
    }

    @Test
    void testCustomCurrentProject() throws Exception {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact("test", "test-webapp");
        final ArtifactStub second = newWarArtifact("test", "test-webapp-2");
        project.addArtifact(first);
        project.addArtifact(second);

        final List<Overlay> overlays = new ArrayList<>();
        overlays.add(new DefaultOverlay(first));
        final Overlay currentProjectOverlay = Overlay.createInstance();
        overlays.add(currentProjectOverlay);

        OverlayManager manager =
                new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay);
        assertNotNull(manager.getOverlays());
        assertEquals(3, manager.getOverlays().size());
        assertEquals(overlays.get(0), manager.getOverlays().get(0));
        assertEquals(currentProjectOverlay, manager.getOverlays().get(1));
        assertEquals(new DefaultOverlay(second), manager.getOverlays().get(2));
    }

    @Test
    void testOverlaysWithSameArtifactAndGroupId() throws Exception {

        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final ArtifactStub first = newWarArtifact("test", "test-webapp");
        final ArtifactStub second = newWarArtifact("test", "test-webapp", "my-classifier");

        project.addArtifact(first);
        project.addArtifact(second);

        final List<Overlay> overlays = new ArrayList<>();
        overlays.add(new DefaultOverlay(first));
        overlays.add(new DefaultOverlay(second));

        final Overlay currentProjectOverlay = Overlay.createInstance();
        OverlayManager manager =
                new OverlayManager(overlays, project, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, currentProjectOverlay);
        assertNotNull(manager.getOverlays());
        assertEquals(3, manager.getOverlays().size());
        assertEquals(currentProjectOverlay, manager.getOverlays().get(0));
        assertEquals(overlays.get(0), manager.getOverlays().get(1));
        assertEquals(overlays.get(1), manager.getOverlays().get(2));
    }

    protected ArtifactStub newWarArtifact(String groupId, String artifactId, String classifier) {
        final WarArtifactStub a = new WarArtifactStub("");
        a.setGroupId(groupId);
        a.setArtifactId(artifactId);
        if (classifier != null) {
            a.setClassifier(classifier);
        }
        return a;
    }

    protected ArtifactStub newWarArtifact(String groupId, String artifactId) {
        return newWarArtifact(groupId, artifactId, null);
    }
}
