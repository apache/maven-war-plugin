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
package org.apache.maven.plugins.war.util;

import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

/**
 * @author Stephane Nicoll
 */
public class WarUtils {

    /**
     * @param project {@link MavenProject}
     * @param dependency {@link Dependency}
     * @return {@link Artifact}
     */
    public static Artifact getArtifact(MavenProject project, Dependency dependency) {
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getGroupId().equals(dependency.getGroupId())
                    && artifact.getArtifactId().equals(dependency.getArtifactId())
                    && artifact.getType().equals(dependency.getType())) {
                if (artifact.getClassifier() == null && dependency.getClassifier() == null) {
                    return artifact;
                } else if (dependency.getClassifier() != null
                        && dependency.getClassifier().equals(artifact.getClassifier())) {
                    return artifact;
                }
            }
        }
        return null;
    }

    /**
     * @param artifact {@link Artifact}
     * @param dependency {@link Dependency}
     * @return is related or not.
     */
    public static boolean isRelated(Artifact artifact, Dependency dependency) {
        if (artifact == null || dependency == null) {
            return false;
        }

        if (!Objects.equals(artifact.getGroupId(), dependency.getGroupId())) {
            return false;
        }
        if (!Objects.equals(artifact.getArtifactId(), dependency.getArtifactId())) {
            return false;
        }
        if (Objects.equals(artifact.getVersion(), dependency.getVersion())) {
            return false;
        }
        if (Objects.equals(artifact.getType(), dependency.getType())) {
            return false;
        }
        if (Objects.equals(artifact.getClassifier(), dependency.getClassifier())) {
            return false;
        }
        if (Objects.equals(artifact.getScope(), dependency.getScope())) {
            return false;
        }
        if (artifact.isOptional() != dependency.isOptional()) {
            return false;
        }

        return true;
    }
}
