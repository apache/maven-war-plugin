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
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.plugins.war.Overlay;

/**
 * Manages the overlays.
 *
 * @author Stephane Nicoll
 */
public class OverlayManager {
    private final List<Overlay> overlays;

    private final Project project;

    private final Session session;

    private final List<DownloadedArtifact> artifactsOverlays;

    /**
     * Creates a manager with the specified overlays.
     *
     * Note that the list is potentially updated by the manager so a new list is created based on the overlays.
     *
     * @param overlays the overlays
     * @param project the maven project
     * @param session the maven session
     * @param defaultIncludes the default includes to use
     * @param defaultExcludes the default excludes to use
     * @param currentProjectOverlay the overlay for the current project
     * @throws InvalidOverlayConfigurationException if the config is invalid
     */
    public OverlayManager(
            List<Overlay> overlays,
            Project project,
            Session session,
            String[] defaultIncludes,
            String[] defaultExcludes,
            Overlay currentProjectOverlay)
            throws InvalidOverlayConfigurationException {
        this.overlays = new ArrayList<>();
        if (overlays != null) {
            this.overlays.addAll(overlays);
        }
        this.project = project;
        this.session = session;

        this.artifactsOverlays = getOverlaysAsArtifacts();

        // Initialize
        initialize(defaultIncludes, defaultExcludes, currentProjectOverlay);
    }

    /**
     * Returns the resolved overlays.
     *
     * @return the overlays
     */
    public List<Overlay> getOverlays() {
        return overlays;
    }

    /**
     * Returns the id of the resolved overlays.
     *
     * @return the overlay ids
     */
    public List<String> getOverlayIds() {
        final List<String> result = new ArrayList<>();
        for (Overlay overlay : overlays) {
            result.add(overlay.getId());
        }
        return result;
    }

    /**
     * Initializes the manager and validates the overlays configuration.
     *
     * @param defaultIncludes the default includes to use
     * @param defaultExcludes the default excludes to use
     * @param currentProjectOverlay the overlay for the current project
     * @throws InvalidOverlayConfigurationException if the configuration is invalid
     */
    void initialize(String[] defaultIncludes, String[] defaultExcludes, Overlay currentProjectOverlay)
            throws InvalidOverlayConfigurationException {

        // Build the list of configured artifacts and makes sure that each overlay
        // refer to a valid artifact
        final List<DownloadedArtifact> configuredWarArtifacts = new ArrayList<>();
        final ListIterator<Overlay> it = overlays.listIterator();
        while (it.hasNext()) {
            Overlay overlay = it.next();
            if (overlay == null) {
                throw new InvalidOverlayConfigurationException("overlay could not be null.");
            }
            // If it's the current project, return the project instance
            if (overlay.isCurrentProject()) {
                overlay = currentProjectOverlay;
                it.set(overlay);
            }
            // default includes/excludes - only if the overlay uses the default settings
            if (Arrays.equals(Overlay.DEFAULT_INCLUDES, overlay.getIncludes())
                    && Arrays.equals(Overlay.DEFAULT_EXCLUDES, overlay.getExcludes())) {
                overlay.setIncludes(defaultIncludes);
                overlay.setExcludes(defaultExcludes);
            }

            final DownloadedArtifact artifact = getAssociatedArtifact(overlay);
            if (artifact != null) {
                configuredWarArtifacts.add(artifact);
                overlay.setArtifact(artifact);
            }
        }

        // Build the list of missing overlays
        for (DownloadedArtifact artifact : artifactsOverlays) {
            if (!configuredWarArtifacts.contains(artifact)) {
                // Add a default overlay for the given artifact which will be applied after
                // the ones that have been configured
                overlays.add(new DefaultOverlay(artifact, defaultIncludes, defaultExcludes));
            }
        }

        // Final validation, make sure that the current project is in there. Otherwise add it first
        for (Overlay overlay : overlays) {
            if (overlay.equals(currentProjectOverlay)) {
                return;
            }
        }
        overlays.add(0, currentProjectOverlay);
    }

    /**
     * Returns the Artifact associated to the specified overlay.
     *
     * If the overlay defines the current project, {@code null} is returned. If no artifact could not be found for the
     * overlay a InvalidOverlayConfigurationException is thrown.
     *
     * @param overlay an overlay
     * @return the artifact associated to the overlay
     * @throws InvalidOverlayConfigurationException if the overlay does not have an
     *             associated artifact
     */
    DownloadedArtifact getAssociatedArtifact(final Overlay overlay) throws InvalidOverlayConfigurationException {
        if (overlay.isCurrentProject()) {
            return null;
        }

        for (DownloadedArtifact artifact : artifactsOverlays) {
            // Handle classifier dependencies properly (clash management)
            if (compareOverlayWithArtifact(overlay, artifact)) {
                return artifact;
            }
        }

        // maybe its a project dependencies zip or an other type
        // Check all resolved dependencies
        List<DownloadedArtifact> allArtifacts = getAllResolvedArtifacts();
        for (DownloadedArtifact artifact : allArtifacts) {
            if (compareOverlayWithArtifact(overlay, artifact)) {
                return artifact;
            }
        }
        // CHECKSTYLE_OFF: LineLength
        throw new InvalidOverlayConfigurationException("overlay [" + overlay + "] is not a dependency of the project.");
        // CHECKSTYLE_ON: LineLength

    }

    /**
     * Compare groupId && artifactId && type && classifier.
     *
     * @param overlay the overlay
     * @param artifact the artifact
     * @return boolean true if equals
     */
    private boolean compareOverlayWithArtifact(Overlay overlay, DownloadedArtifact artifact) {
        String oc = overlay.getClassifier() == null ? "" : overlay.getClassifier();
        String ac = artifact.getClassifier() == null ? "" : artifact.getClassifier();
        return (Objects.equals(overlay.getGroupId(), artifact.getGroupId())
                && Objects.equals(overlay.getArtifactId(), artifact.getArtifactId())
                && Objects.equals(overlay.getType(), artifact.getExtension())
                // MWAR-241 Make sure to treat null and "" as equal when comparing the classifier
                && Objects.equals(oc, ac));
    }

    /**
     * Returns a list of WAR {@link DownloadedArtifact} describing the overlays of the current project.
     *
     * @return the overlays as artifacts objects
     */
    private List<DownloadedArtifact> getOverlaysAsArtifacts() {
        final List<DownloadedArtifact> result = new ArrayList<>();
        Node root = session.collectDependencies(project, PathScope.MAIN_RUNTIME);
        if (root != null) {
            collectArtifacts(root, result, true);
        }
        return result;
    }

    /**
     * Returns all resolved dependency artifacts.
     */
    private List<DownloadedArtifact> getAllResolvedArtifacts() {
        final List<DownloadedArtifact> result = new ArrayList<>();
        Node root = session.collectDependencies(project, PathScope.MAIN_RUNTIME);
        if (root != null) {
            collectArtifacts(root, result, false);
        }
        return result;
    }

    /**
     * Walks the dependency tree and collects resolved artifacts.
     * Uses manual tree walking instead of {@code flattenDependencies} because the latter
     * filters out non-classpath types (like war), which we need for overlay resolution.
     *
     * @param node the root node
     * @param result the list to collect into
     * @param warOnly if true, only collect WAR artifacts with runtime-compatible scope
     */
    private void collectArtifacts(Node node, List<DownloadedArtifact> result, boolean warOnly) {
        org.apache.maven.api.Dependency dep = node.getDependency();
        if (dep != null) {
            boolean include = !warOnly
                    || (!dep.isOptional() && isRuntimeScope(dep.getScope()) && "war".equals(dep.getExtension()));
            if (include) {
                try {
                    DownloadedArtifact resolved = session.resolveArtifact(dep);
                    result.add(resolved);
                } catch (Exception e) {
                    // Skip unresolvable artifacts
                }
            }
        }
        if (node.getChildren() != null) {
            for (Node child : node.getChildren()) {
                if (child != null) {
                    collectArtifacts(child, result, warOnly);
                }
            }
        }
    }

    /**
     * Checks if the given scope is a runtime-compatible scope
     * (equivalent to the old ScopeArtifactFilter with RUNTIME scope).
     */
    private boolean isRuntimeScope(DependencyScope scope) {
        return scope == DependencyScope.COMPILE || scope == DependencyScope.RUNTIME || scope == DependencyScope.SYSTEM;
    }
}
