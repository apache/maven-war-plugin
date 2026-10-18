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

import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DependencyInfoTest {

    @Test
    void equalsShouldBeBasedOnDependencyOnly() {
        Dependency dep = new Dependency();
        dep.setGroupId("g");
        dep.setArtifactId("a");
        dep.setVersion("1.0");

        DependencyInfo info1 = new DependencyInfo(dep);
        DependencyInfo info2 = new DependencyInfo(dep);
        info2.setTargetFileName("different.txt");

        assertEquals(info1, info2);
    }

    @Test
    void hashCodeShouldBeConsistentWithEquals() {
        Dependency dep = new Dependency();
        dep.setGroupId("g");
        dep.setArtifactId("a");
        dep.setVersion("1.0");

        DependencyInfo info1 = new DependencyInfo(dep);
        DependencyInfo info2 = new DependencyInfo(dep);
        info2.setTargetFileName("different.txt");

        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    void equalsShouldReturnFalseForDifferentDependencies() {
        Dependency dep1 = new Dependency();
        dep1.setGroupId("g");
        dep1.setArtifactId("a");
        dep1.setVersion("1.0");

        Dependency dep2 = new Dependency();
        dep2.setGroupId("g");
        dep2.setArtifactId("b");
        dep2.setVersion("1.0");

        DependencyInfo info1 = new DependencyInfo(dep1);
        DependencyInfo info2 = new DependencyInfo(dep2);

        assertNotEquals(info1, info2);
    }
}
