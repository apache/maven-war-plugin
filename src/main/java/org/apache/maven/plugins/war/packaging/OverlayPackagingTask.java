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

                // Step1b: Remove jars from overlay that conflict with managed dependencies
                filterConflictingDependencyJars(context, tmpDir);

                // Step2: setup
                final PathSet includes = getFilesToIncludes(tmpDir, overlay.getIncludes(), overlay.getExcludes());

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
     * Removes jars from the overlay's {@code WEB-INF/lib} that conflict with the project's managed
     * dependencies. When a project uses dependencyManagement to pin a version, any jar from the
     * overlay with the same artifactId but a different version is removed, ensuring only the
     * dependency-managed version ends up in {@code WEB-INF/lib}.
     *
     * @param context the packaging context
     * @param overlayDir the unpacked overlay directory
     */
    private void filterConflictingDependencyJars(WarPackagingContext context, File overlayDir) {
        File libDir = new File(overlayDir, "WEB-INF/lib");
        if (!libDir.isDirectory()) {
            return;
        }

        ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
        Set<String> projectArtifactIds = new HashSet<>();
        for (Artifact artifact : context.getProject().getArtifacts()) {
            if (!artifact.isOptional() && filter.include(artifact) && "jar".equals(artifact.getType())) {
                projectArtifactIds.add(artifact.getArtifactId());
            }
        }

        if (projectArtifactIds.isEmpty()) {
            return;
        }

        File[] overlayJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (overlayJars == null) {
            return;
        }

        for (File overlayJar : overlayJars) {
            String jarName = overlayJar.getName();
            String artifactId = extractArtifactId(jarName);
            if (artifactId != null && projectArtifactIds.contains(artifactId)) {
                context.getLog()
                        .debug("Removing dependency [" + jarName + "] from overlay [" + overlay.getId()
                                + "]; managed version in project already provides it");
                overlayJar.delete();
            }
        }
    }

    /**
     * Extracts the Maven artifactId from a jar filename following the
     * {@code artifactId-version(-classifier)?.jar} convention.
     *
     * @param jarName the jar filename
     * @return the artifactId, or null if it cannot be determined
     */
    private static String extractArtifactId(String jarName) {
        if (jarName == null || !jarName.endsWith(".jar")) {
            return null;
        }
        String baseName = jarName.substring(0, jarName.length() - 4);
        int versionStart = -1;
        for (int i = 0; i < baseName.length(); i++) {
            if (Character.isDigit(baseName.charAt(i))) {
                versionStart = i;
                break;
            }
        }
        if (versionStart > 0 && baseName.charAt(versionStart - 1) == '-') {
            return baseName.substring(0, versionStart - 1);
        }
        return null;
    }
}
