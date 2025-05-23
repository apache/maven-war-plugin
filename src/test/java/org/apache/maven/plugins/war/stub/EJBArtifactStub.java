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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;

public class EJBArtifactStub extends AbstractArtifactStub {
    protected String groupId;

    public EJBArtifactStub(String basedir) {
        super(basedir);
    }

    public void setGroupId(String id) {
        groupId = id;
    }

    public String getGroupId() {
        if (groupId != null) {
            return groupId;
        } else {
            return "org.sample.ejb";
        }
    }

    public String getType() {
        return "ejb";
    }

    public String getArtifactId() {
        return "ejbartifact";
    }

    public File getFile() {
        return new File(basedir, "/target/test-classes/unit/sample_wars/ejb.jar");
    }

    public ArtifactHandler getArtifactHandler() {
        return new DefaultArtifactHandler() {
            public String getExtension() {
                return "jar";
            }
        };
    }
}
