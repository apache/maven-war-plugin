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
package org.apache.maven.plugins.war.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.Overlay;
import org.apache.maven.plugins.war.util.PathSet;
import org.codehaus.plexus.util.FileUtils;

/**
 * Handles an overlay.
 *
 * @author Stephane Nicoll
 */
public class OverlayPackagingTask extends AbstractWarPackagingTask {
    private final Overlay overlay;

    /**
     * @param overlay {@link #overlay}
     * @param currentProjectOverlay current overlay
     */
    public OverlayPackagingTask(Overlay overlay, Overlay currentProjectOverlay) {
        if (overlay == null) {
            throw new NullPointerException("overlay could not be null.");
        }
        if (overlay.equals(currentProjectOverlay)) {
            throw new IllegalStateException("Could not handle the current project with this task.");
        }
        this.overlay = overlay;
    }

    @Override
    public void performPackaging(WarPackagingContext context) throws MojoExecutionException {
        context.getLog()
                .debug("OverlayPackagingTask performPackaging overlay.getTargetPath() " + overlay.getTargetPath());
        if (overlay.shouldSkip()) {
            context.getLog().info("Skipping overlay [" + overlay + "]");
        } else {
            try {
                context.getLog().info("Processing overlay [" + overlay + "]");

                // Step1: Extract if necessary
                final File tmpDir = unpackOverlay(context, overlay);

                // Step1b: Identify jars from overlay that conflict with project dependencies
                Set<String> conflictingJars = computeConflictingJars(context, tmpDir);

                // Step2: setup, excluding conflicting jars so the cached overlay is not mutated
                String[] effectiveExcludes = mergeExcludes(overlay.getExcludes(), conflictingJars);
                final PathSet includes = getFilesToIncludes(tmpDir, overlay.getIncludes(), effectiveExcludes);

                // Copy
                if (null == overlay.getTargetPath()) {
                    copyFiles(overlay.getId(), context, tmpDir, includes, overlay.isFiltered());
                } else {
                    // overlay.getTargetPath() must ended with /
                    // if not we add it
                    String targetPath = overlay.getTargetPath();
                    if (!targetPath.endsWith("/")) {
                        targetPath = targetPath + "/";
                    }
                    copyFiles(overlay.getId(), context, tmpDir, includes, targetPath, overlay.isFiltered());
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy file for overlay [" + overlay + "]", e);
            }
        }
    }

    /**
     * Unpacks the specified overlay.
     *
     * Makes sure to skip the unpack process if the overlay has already been unpacked.
     *
     * @param context the packaging context
     * @param overlay the overlay
     * @return the directory containing the unpacked overlay
     * @throws MojoExecutionException if an error occurred while unpacking the overlay
     */
    protected File unpackOverlay(WarPackagingContext context, Overlay overlay) throws MojoExecutionException {
        final File tmpDir = getOverlayTempDirectory(context, overlay);

        // TODO: not sure it's good, we should reuse the markers of the dependency plugin
        if (FileUtils.sizeOfDirectory(tmpDir) == 0
                || overlay.getArtifact().getFile().lastModified() > tmpDir.lastModified()) {
            doUnpack(context, overlay.getArtifact().getFile(), tmpDir);
        } else {
            context.getLog().debug("Overlay [" + overlay + "] was already unpacked");
        }
        return tmpDir;
    }

    /**
     * Returns the directory to use to unpack the specified overlay.
     *
     * @param context the packaging context
     * @param overlay the overlay
     * @return the temp directory for the overlay
     */
    protected File getOverlayTempDirectory(WarPackagingContext context, Overlay overlay) {
        final File groupIdDir = new File(context.getOverlaysWorkDirectory(), overlay.getGroupId());
        if (!groupIdDir.exists()) {
            groupIdDir.mkdir();
        }
        String directoryName = overlay.getArtifactId();
        if (overlay.getClassifier() != null) {
            directoryName = directoryName + "-" + overlay.getClassifier();
        }
        final File result = new File(groupIdDir, directoryName);
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }

    /**
     * Identifies jars from the overlay's {@code WEB-INF/lib} whose artifactId matches a non-optional
     * runtime-scope dependency resolved by the project. These jars are candidates for exclusion
     * from the overlay copy so that only the project-resolved version ends up in {@code WEB-INF/lib}.
     *
     * <p>The overlay's cached unpack directory is <em>not</em> mutated; the returned set is used
     * as additional excludes during the copy step.
     *
     * @param context the packaging context
     * @param overlayDir the unpacked overlay directory
     * @return set of paths (relative to the overlay dir) to exclude from the overlay copy
     */
    private Set<String> computeConflictingJars(WarPackagingContext context, File overlayDir) {
        File libDir = new File(overlayDir, "WEB-INF/lib");
        if (!libDir.isDirectory()) {
            return Collections.emptySet();
        }

        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
        Set<String> projectArtifactIds = new HashSet<>();
        for (Artifact artifact : context.getProject().getArtifacts()) {
            if (!artifact.isOptional() && filter.include(artifact) && "jar".equals(artifact.getType())) {
                projectArtifactIds.add(artifact.getArtifactId());
            }
        }

        if (projectArtifactIds.isEmpty()) {
            return Collections.emptySet();
        }

        File[] overlayJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (overlayJars == null) {
            return Collections.emptySet();
        }

        Set<String> conflicting = new HashSet<>();
        for (File overlayJar : overlayJars) {
            String jarName = overlayJar.getName();
            String artifactId = extractArtifactId(jarName);
            if (artifactId != null && projectArtifactIds.contains(artifactId)) {
                context.getLog()
                        .debug("Excluding dependency [" + jarName + "] from overlay [" + overlay.getId()
                                + "]; project runtime dependencies already include a version");
                conflicting.add("WEB-INF/lib/" + jarName);
            }
        }
        return conflicting;
    }

    /**
     * Merges the overlay's existing excludes with additional jar filenames to exclude.
     *
     * @param overlayExcludes excludes from the overlay configuration, may be null
     * @param additionalJars paths to add as excludes
     * @return merged excludes array
     */
    private static String[] mergeExcludes(String[] overlayExcludes, Set<String> additionalJars) {
        if (additionalJars.isEmpty()) {
            return overlayExcludes;
        }
        if (overlayExcludes == null || overlayExcludes.length == 0) {
            return additionalJars.toArray(new String[0]);
        }
        String[] merged = Arrays.copyOf(overlayExcludes, overlayExcludes.length + additionalJars.size());
        System.arraycopy(
                additionalJars.toArray(new String[0]), 0, merged, overlayExcludes.length, additionalJars.size());
        return merged;
    }

    /**
     * Extracts the Maven artifactId from a jar filename following the
     * {@code artifactId-version(-classifier)?.jar} convention.
     *
     * <p>Scans right-to-left for the last {@code -} followed by a digit, which correctly
     * handles artifactIds that contain digits (e.g. {@code commons-lang3-3.20.0.jar}).
     *
     * @param jarName the jar filename
     * @return the artifactId, or null if it cannot be determined
     */
    private static String extractArtifactId(String jarName) {
        if (jarName == null || !jarName.endsWith(".jar")) {
            return null;
        }
        String baseName = jarName.substring(0, jarName.length() - 4);
        for (int i = baseName.length() - 2; i >= 0; i--) {
            if (baseName.charAt(i) == '-' && Character.isDigit(baseName.charAt(i + 1))) {
                return baseName.substring(0, i);
            }
        }
        return null;
    }
}
