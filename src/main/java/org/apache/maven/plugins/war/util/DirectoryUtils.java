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
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Helper class to handle directories.
 */
public class DirectoryUtils {

    /**
     * Creates a directory if it does not exist (at all).
     *
     * @param toBeCreated The directory to be created
     * @throws MojoExecutionException if the path already exists but is not a directory, or if the directory
     *                                does not exist <b>and</b> could not be created
     */
    public static void createDirectoryIfNotExists(Path toBeCreated) throws MojoExecutionException {
        if (Files.exists(toBeCreated)) {
            if (!Files.isDirectory(toBeCreated)) {
                throw new MojoExecutionException("Could not create directory " + toBeCreated
                        + " because a file with the same name already exists");
            }
        } else {
            try {
                Files.createDirectories(toBeCreated);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create directory " + toBeCreated, e);
            }
        }
    }
}
