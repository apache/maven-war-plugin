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
package org.apache.maven.plugins.war.stub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Helper to configure mock Session objects for Maven 4 dependency resolution in tests.
 * <p>
 * In Maven 4, dependencies are resolved through the Session rather than through
 * Project.getArtifacts(). This helper configures the mock Session (created by MojoExtension)
 * so that collectDependencies/flattenDependencies/resolveArtifact return the expected
 * test artifacts.
 */
public final class MockSessionHelper {

    private MockSessionHelper() {}

    /**
     * Configures the mock session to return the given artifacts as resolved dependencies.
     *
     * @param session the mock Session (from MojoExtension)
     * @param artifacts the test artifact stubs to register as dependencies
     */
    @SuppressWarnings("unchecked")
    public static void registerArtifacts(Session session, List<? extends AbstractArtifactStub> artifacts) {
        Node rootNode = mock(Node.class);
        lenient()
                .when(session.collectDependencies(any(Project.class), any(PathScope.class)))
                .thenReturn(rootNode);

        // Map from dependency mock identity to the corresponding artifact stub
        Map<Object, DownloadedArtifact> resolveMap = new HashMap<>();

        List<Node> nodes = new ArrayList<>();
        for (AbstractArtifactStub artifact : artifacts) {
            Node node = mock(Node.class);
            org.apache.maven.api.Dependency dep = mock(org.apache.maven.api.Dependency.class);
            lenient().when(dep.getGroupId()).thenReturn(artifact.getGroupId());
            lenient().when(dep.getArtifactId()).thenReturn(artifact.getArtifactId());
            lenient().when(dep.getVersion()).thenReturn(artifact.getVersion());
            lenient().when(dep.getExtension()).thenReturn(artifact.getExtension());
            lenient().when(dep.getClassifier()).thenReturn(artifact.getClassifier());

            // Map scope string to DependencyScope enum
            DependencyScope scope = toDependencyScope(artifact.getScope());
            lenient().when(dep.getScope()).thenReturn(scope);
            lenient().when(dep.isOptional()).thenReturn(artifact.isOptional());

            lenient().when(node.getDependency()).thenReturn(dep);

            // Track this mock -> artifact mapping for resolveArtifact
            resolveMap.put(dep, artifact);

            nodes.add(node);
        }
        lenient()
                .when(session.flattenDependencies(any(Node.class), any(PathScope.class)))
                .thenReturn((List) nodes);

        // In Maven 4, Dependency extends Artifact (not ArtifactCoordinates),
        // so resolveArtifact dispatches to the resolveArtifact(Artifact) overload.
        lenient().when(session.resolveArtifact(any(Artifact.class))).thenAnswer(invocation -> {
            Artifact arg = invocation.getArgument(0);
            DownloadedArtifact result = resolveMap.get(arg);
            if (result != null) {
                return result;
            }
            // Fallback: match by coordinates
            for (Map.Entry<Object, DownloadedArtifact> entry : resolveMap.entrySet()) {
                DownloadedArtifact a = entry.getValue();
                if (a.getGroupId().equals(arg.getGroupId()) && a.getArtifactId().equals(arg.getArtifactId())) {
                    return a;
                }
            }
            return null;
        });
    }

    private static DependencyScope toDependencyScope(String scope) {
        if (scope == null || scope.isEmpty() || "compile".equals(scope)) {
            return DependencyScope.COMPILE;
        }
        switch (scope) {
            case "runtime":
                return DependencyScope.RUNTIME;
            case "provided":
                return DependencyScope.PROVIDED;
            case "system":
                return DependencyScope.SYSTEM;
            case "test":
                return DependencyScope.TEST;
            default:
                return DependencyScope.COMPILE;
        }
    }
}
