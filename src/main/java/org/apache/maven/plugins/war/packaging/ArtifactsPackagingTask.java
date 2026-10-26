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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.Overlay;
import org.codehaus.plexus.interpolation.InterpolationException;

/**
 * Handles the artifacts that needs to be packaged in the web application.
 *
 * @author Stephane Nicoll
 */
public class ArtifactsPackagingTask extends AbstractWarPackagingTask {

    /**
     * The {@code tld} path.
     */
    public static final String TLD_PATH = "WEB-INF/tld/";

    /**
     * The {@code services} path.
     */
    public static final String SERVICES_PATH = "WEB-INF/services/";

    /**
     * The {@code modules} path.
     */
    public static final String MODULES_PATH = "WEB-INF/modules/";

    /**
     * The {@code extensions} path.
     */
    public static final String EXTENSIONS_PATH = "WEB-INF/extensions/";

    private final Set<Artifact> artifacts;

    private final String id;

    /**
     * @param artifacts {@link #artifacts}
     * @param currentProjectOverlay {@link #id}
     */
    public ArtifactsPackagingTask(Set<Artifact> artifacts, Overlay currentProjectOverlay) {
        this.artifacts = artifacts;
        this.id = currentProjectOverlay.getId();
    }

    @Override
    public void performPackaging(WarPackagingContext context) throws MojoExecutionException {
        try {
            final ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
            final List<Artifact> orderedArtifacts = new ArrayList<>(artifacts);
            final List<String> finalNames = new ArrayList<>(orderedArtifacts.size());
            for (Artifact artifact : orderedArtifacts) {
                finalNames.add(getArtifactFinalName(context, artifact));
            }
            final Set<String> duplicates = findDuplicates(finalNames);

            for (int i = 0; i < orderedArtifacts.size(); i++) {
                Artifact artifact = orderedArtifacts.get(i);
                String targetFileName = finalNames.get(i);

                context.getLog().debug("Processing: " + targetFileName);

                if (duplicates.contains(targetFileName)) {
                    context.getLog().debug("Duplicate found: " + targetFileName);
                    targetFileName = artifact.getGroupId() + "-" + targetFileName;
                    context.getLog().debug("Renamed to: " + targetFileName);
                }
                context.getWebappStructure().registerTargetFileName(artifact, targetFileName);

                if (!artifact.isOptional() && filter.include(artifact)) {
                    try {
                        String type = artifact.getType();
                        if ("tld".equals(type)) {
                            copyFile(id, context, artifact.getFile(), TLD_PATH + targetFileName);
                        } else if ("aar".equals(type)) {
                            copyFile(id, context, artifact.getFile(), SERVICES_PATH + targetFileName);
                        } else if ("mar".equals(type)) {
                            copyFile(id, context, artifact.getFile(), MODULES_PATH + targetFileName);
                        } else if ("xar".equals(type)) {
                            copyFile(id, context, artifact.getFile(), EXTENSIONS_PATH + targetFileName);
                        } else if ("jar".equals(type)
                                || "ejb".equals(type)
                                || "ejb-client".equals(type)
                                || "test-jar".equals(type)
                                || "bundle".equals(type)) {
                            copyFile(id, context, artifact.getFile(), LIB_PATH + targetFileName);
                        } else if ("par".equals(type)) {
                            targetFileName = targetFileName.substring(0, targetFileName.lastIndexOf('.')) + ".jar";
                            copyFile(id, context, artifact.getFile(), LIB_PATH + targetFileName);
                        } else if ("war".equals(type)) {
                            // Nothing to do here, it is an overlay and it's already handled
                            context.getLog()
                                    .debug("war artifacts are handled as overlays, ignoring [" + artifact + "]");
                        } else if ("zip".equals(type)) {
                            // Nothing to do here, it is an overlay and it's already handled
                            context.getLog()
                                    .debug("zip artifacts are handled as overlays, ignoring [" + artifact + "]");
                        } else {
                            context.getLog()
                                    .debug("Artifact of type [" + type + "] is not supported, ignoring [" + artifact
                                            + "]");
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to copy file for artifact [" + artifact + "]", e);
                    }
                }
            }
        } catch (InterpolationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Returns the file names that occur more than once.
     *
     * @param finalNames the final file names of the artifacts
     * @return the duplicated file names
     */
    private static Set<String> findDuplicates(Iterable<String> finalNames) {
        Set<String> duplicates = new HashSet<>();
        Set<String> identifiers = new HashSet<>();
        for (String candidate : finalNames) {
            if (!identifiers.add(candidate)) {
                duplicates.add(candidate);
            }
        }
        return duplicates;
    }
}
