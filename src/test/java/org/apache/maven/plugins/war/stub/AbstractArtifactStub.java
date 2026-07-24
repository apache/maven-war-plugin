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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.plugin.testing.stubs.ArtifactStub;

public abstract class AbstractArtifactStub extends ArtifactStub implements DownloadedArtifact {
    protected String basedir;

    private String scope = "runtime";

    private boolean optional = false;

    public AbstractArtifactStub(String basedir) {
        this.basedir = basedir;
        setVersion("0.0-Test");
    }

    /**
     * Returns the type/extension of this artifact. Subclasses should override.
     */
    public String getType() {
        return getExtension();
    }

    /**
     * Returns the file for this artifact. Subclasses should override.
     */
    public File getFile() {
        return null;
    }

    @Override
    public Path getPath() {
        File f = getFile();
        return f != null ? f.toPath() : null;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setFile(File file) {
        // Default no-op, subclasses with mutable file override this
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroupId(), getArtifactId(), getExtension(), getClassifier());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractArtifactStub)) {
            return false;
        }
        AbstractArtifactStub a = (AbstractArtifactStub) o;
        return Objects.equals(getGroupId(), a.getGroupId())
                && Objects.equals(getArtifactId(), a.getArtifactId())
                && Objects.equals(getExtension(), a.getExtension())
                && Objects.equals(getClassifier(), a.getClassifier());
    }
}
