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

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Stephane Nicoll
 */
public class WebappStructureTest {
    @Test
    public void testUnknownFileNotAvailable() {
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        assertFalse(structure.isRegistered("/foo/bar.txt"));
    }

    @Test
    public void testRegisterSamePathTwice() {
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        structure.registerFile("overlay1", "WEB-INF/web.xml");
        assertFalse(structure.registerFile("currentBuild", "WEB-INF/web.xml"));
    }

    @Test
    public void testRegisterForced() {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        assertFalse(structure.registerFileForced("overlay1", path), "New file should return false");
        assertEquals("overlay1", structure.getOwner(path));
    }

    @Test
    public void testRegisterSamePathTwiceForced() {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure(new ArrayList<>());
        structure.registerFile("overlay1", path);
        assertEquals("overlay1", structure.getOwner(path));
        assertTrue(structure.registerFileForced("currentBuild", path), "owner replacement should have returned true");
        assertEquals("currentBuild", structure.getOwner(path));
    }

    // ... existing code ...
}
