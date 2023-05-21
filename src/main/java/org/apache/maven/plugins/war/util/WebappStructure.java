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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 * Represents the structure of a web application composed of multiple overlays. Each overlay is registered within this
 * structure with the set of files it holds.
 *
 * Note that this structure is persisted to disk at each invocation to store which owner holds which path (file).
 *
 * @author Stephane Nicoll
 */
public class WebappStructure {

    private Map<String, PathSet> registeredFiles;

    private List<DependencyInfo> dependenciesInfo;

    private transient PathSet allFiles = new PathSet();

    /**
     * Creates a new empty instance.
     *
     * @param dependencies the dependencies of the project
     */
    public WebappStructure(List<Dependency> dependencies) {
        this.dependenciesInfo = createDependenciesInfoList(dependencies);
        this.registeredFiles = new HashMap<>();
    }

    /**
     * Returns the list of {@link DependencyInfo} for the project.
     *
     * @return the dependencies information of the project
     */
    public List<DependencyInfo> getDependenciesInfo() {
        return dependenciesInfo;
    }

    /**
     * Returns the dependencies of the project.
     *
     * @return the dependencies of the project
     */
    public List<Dependency> getDependencies() {
        final List<Dependency> result = new ArrayList<>();
        if (dependenciesInfo == null) {
            return result;
        }
        for (DependencyInfo dependencyInfo : dependenciesInfo) {
            result.add(dependencyInfo.getDependency());
        }
        return result;
    }

    /**
     * Specify if the specified {@code path} is registered or not.
     *
     * @param path the relative path from the webapp root directory
     * @return true if the path is registered, false otherwise
     */
    public boolean isRegistered(String path) {
        return getFullStructure().contains(path);
    }

    /**
     * Registers the specified path for the specified owner. Returns {@code true} if the path is not already
     * registered, {@code false} otherwise.
     *
     * @param id the owner of the path
     * @param path the relative path from the webapp root directory
     * @return true if the file was registered successfully
     */
    public boolean registerFile(String id, String path) {
        if (!isRegistered(path)) {
            doRegister(id, path);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Forces the registration of the specified path for the specified owner. If the file is not registered yet, a
     * simple registration is performed. If the file already exists, the owner changes to the specified one.
     * <p>
     * Beware that the semantic of the return boolean is different than the one from
     * {@link #registerFile(String, String)}; returns {@code true} if an owner replacement was made and {@code false}
     * if the file was simply registered for the first time.</p>
     *
     * @param id the owner of the path
     * @param path the relative path from the webapp root directory
     * @return false if the file did not exist, true if the owner was replaced
     */
    public boolean registerFileForced(String id, String path) {
        if (!isRegistered(path)) {
            doRegister(id, path);
            return false;
        } else {
            // Force the switch to the new owner
            getStructure(getOwner(path)).remove(path);
            getStructure(id).add(path);
            return true;
        }
    }

    /**
     * Registers the specified path for the specified owner. Invokes the {@code callback} with the result of the
     * registration.
     *
     * @param id the owner of the path
     * @param path the relative path from the webapp root directory
     * @param callback the callback to invoke with the result of the registration
     * @throws IOException if the callback invocation throws an IOException
     */
    public void registerFile(String id, String path, RegistrationCallback callback) throws IOException {

        // If the file is already in the current structure, rejects it with the current owner
        if (isRegistered(path)) {
            callback.refused(id, path, getOwner(path));
        } else {
            doRegister(id, path);
            // This is a new file
            if (getOwner(path) == null) {
                callback.registered(id, path);

            } // The file already belonged to this owner
            else if (getOwner(path).equals(id)) {
                callback.alreadyRegistered(id, path);
            } // The file belongs to another owner and it's known currently
            else if (getOwners().contains(getOwner(path))) {
                callback.superseded(id, path, getOwner(path));
            } // The file belongs to another owner and it's unknown
            else {
                callback.supersededUnknownOwner(id, path, getOwner(path));
            }
        }
    }

    /**
     * Returns the owner of the specified {@code path}. If the file is not registered, returns {@code null}
     *
     * @param path the relative path from the webapp root directory
     * @return the owner or {@code null}.
     */
    public String getOwner(String path) {
        if (!isRegistered(path)) {
            return null;
        } else {
            for (final String owner : registeredFiles.keySet()) {
                final PathSet structure = getStructure(owner);
                if (structure.contains(path)) {
                    return owner;
                }
            }
            throw new IllegalStateException(
                    "Should not happen, path [" + path + "] is flagged as being registered but was not found.");
        }
    }

    /**
     * Returns the owners.
     *
     * @return the list of owners
     */
    public Set<String> getOwners() {
        return registeredFiles.keySet();
    }

    /**
     * Returns all paths that have been registered so far.
     *
     * @return all registered path
     */
    public PathSet getFullStructure() {
        return allFiles;
    }

    /**
     * Returns the list of registered files for the specified owner.
     *
     * @param id the owner
     * @return the list of files registered for that owner
     */
    public PathSet getStructure(String id) {
        PathSet pathSet = registeredFiles.get(id);
        if (pathSet == null) {
            pathSet = new PathSet();
            registeredFiles.put(id, pathSet);
        }
        return pathSet;
    }

    /**
     * Registers the target file name for the specified artifact.
     *
     * @param artifact the artifact
     * @param targetFileName the target file name
     */
    public void registerTargetFileName(Artifact artifact, String targetFileName) {
        if (dependenciesInfo != null) {
            for (DependencyInfo dependencyInfo : dependenciesInfo) {
                if (WarUtils.isRelated(artifact, dependencyInfo.getDependency())) {
                    dependencyInfo.setTargetFileName(targetFileName);
                }
            }
        }
    }

    // Private helpers

    private void doRegister(String id, String path) {
        getFullStructure().add(path);
        getStructure(id).add(path);
    }

    private List<DependencyInfo> createDependenciesInfoList(List<Dependency> dependencies) {
        if (dependencies == null) {
            return Collections.emptyList();
        }
        final List<DependencyInfo> result = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            result.add(new DependencyInfo(dependency));
        }
        return result;
    }

    private Object readResolve() {
        // the full structure should be resolved so let's rebuild it
        this.allFiles = new PathSet();
        for (PathSet pathSet : registeredFiles.values()) {
            this.allFiles.addAll(pathSet);
        }
        return this;
    }

    /**
     * Callback interface to handle events related to filepath registration in the webapp.
     */
    public interface RegistrationCallback {

        /**
         * Called if the {@code targetFilename} for the specified {@code ownerId} has been registered successfully.
         *
         * This means that the {@code targetFilename} was unknown and has been registered successfully.
         *
         * @param ownerId the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @throws IOException if an error occurred while handling this event
         */
        void registered(String ownerId, String targetFilename) throws IOException;

        /**
         * Called if the {@code targetFilename} for the specified {@code ownerId} has already been registered.
         *
         * This means that the {@code targetFilename} was known and belongs to the specified owner.
         *
         * @param ownerId the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @throws IOException if an error occurred while handling this event
         */
        void alreadyRegistered(String ownerId, String targetFilename) throws IOException;

        /**
         * <p>
         * Called if the registration of the {@code targetFilename} for the specified {@code ownerId} has been refused
         * since the path already belongs to the {@code actualOwnerId}.
         * </p>
         * This means that the {@code targetFilename} was known and does not belong to the specified owner.
         *
         * @param ownerId the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @param actualOwnerId the actual owner
         * @throws IOException if an error occurred while handling this event
         */
        void refused(String ownerId, String targetFilename, String actualOwnerId) throws IOException;

        /**
         * Called if the {@code targetFilename} for the specified {@code ownerId} has been registered successfully by
         * superseding a {@code deprecatedOwnerId}, that is the previous owner of the file.
         *
         * This means that the {@code targetFilename} was known but for another owner. This usually happens after a
         * project's configuration change. As a result, the file has been registered successfully to the new owner.
         *
         * @param ownerId the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @param deprecatedOwnerId the previous owner that does not exist anymore
         * @throws IOException if an error occurred while handling this event
         */
        void superseded(String ownerId, String targetFilename, String deprecatedOwnerId) throws IOException;

        /**
         * Called if the {@code targetFilename} for the specified {@code ownerId} has been registered successfully by
         * superseding a {@code unknownOwnerId}, that is an owner that does not exist anymore in the current project.
         *
         * This means that the {@code targetFilename} was known but for an owner that does not exist anymore. Hence the
         * file has been registered successfully to the new owner.
         *
         * @param ownerId the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @param unknownOwnerId the previous owner that does not exist anymore
         * @throws IOException if an error occurred while handling this event
         */
        void supersededUnknownOwner(String ownerId, String targetFilename, String unknownOwnerId) throws IOException;
    }
}
