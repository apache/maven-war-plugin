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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathSetTest {

    /* --------------- Normalization tests --------------*/

    /**
     * Test method for 'org.apache.maven.plugin.war.PathSet.normalizeSubPath(String)'
     */
    @Test
    public void testNormalizeSubPath() {
        assertEquals("", PathSet.normalizeSubPath(""), "Normalized path error");
        assertEquals("", PathSet.normalizeSubPath("/"), "Normalized path error");
        assertEquals("", PathSet.normalizeSubPath("////"), "Normalized path error");
        assertEquals("", PathSet.normalizeSubPath("\\"), "Normalized path error");
        assertEquals("", PathSet.normalizeSubPath("\\\\\\\\"), "Normalized path error");

        assertEquals("abc", PathSet.normalizeSubPath("abc"), "Normalized path error");
        assertEquals("abc", PathSet.normalizeSubPath("/abc"), "Normalized path error");
        assertEquals("abc", PathSet.normalizeSubPath("////abc"), "Normalized path error");
        assertEquals("abc", PathSet.normalizeSubPath("\\abc"), "Normalized path error");
        assertEquals("abc", PathSet.normalizeSubPath("\\\\\\\\abc"), "Normalized path error");

        assertEquals("abc/def/xyz", PathSet.normalizeSubPath("abc/def\\xyz\\"), "Normalized path error");
        assertEquals("abc/def/xyz", PathSet.normalizeSubPath("/abc/def/xyz/"), "Normalized path error");
        assertEquals("abc/def/xyz", PathSet.normalizeSubPath("////abc/def/xyz/"), "Normalized path error");
        assertEquals("abc/def/xyz", PathSet.normalizeSubPath("\\abc/def/xyz/"), "Normalized path error");
        assertEquals("abc/def/xyz", PathSet.normalizeSubPath("\\\\\\\\abc/def/xyz/"), "Normalized path error");
        // MWAR-371
        assertEquals("abc/def/ghi", PathSet.normalizeSubPath("///abc/////def////ghi//"), "Normalized path error");
    }

    /* -------------- Operations tests ------------------*/

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet()</li>
     * <li>org.apache.maven.plugin.war.PathSet.size()</li>
     * <li>org.apache.maven.plugin.war.PathSet.add()</li>
     * <li>org.apache.maven.plugin.war.PathSet.addAll()</li>
     * <li>org.apache.maven.plugin.war.PathSet.iterate()</li>
     * <li>org.apache.maven.plugin.war.PathSet.contains()</li>
     * <li>org.apache.maven.plugin.war.PathSet.addPrefix(String)</li>
     * </ul>
     */
    @Test
    public void testPathsSetBasic() {
        PathSet ps = new PathSet();
        assertEquals(0, ps.size(), "Unexpected PathSet size");
        Iterator<String> iter = ps.iterator();
        assertNotNull(iter, "Iterator is null");
        assertFalse(iter.hasNext(), "Can iterate on empty set");

        ps.add("abc");
        assertEquals(1, ps.size(), "Unexpected PathSet size");
        ps.add("abc");
        assertEquals(1, ps.size(), "Unexpected PathSet size");
        ps.add("xyz/abc");
        assertEquals(2, ps.size(), "Unexpected PathSet size");
        ps.add("///abc");
        assertEquals(2, ps.size(), "Unexpected PathSet size");
        ps.add("///xyz\\abc");
        assertEquals(2, ps.size(), "Unexpected PathSet size");

        ps.addAll(ps);
        assertEquals(2, ps.size(), "Unexpected PathSet size");

        int i = 0;
        for (String pathstr : ps) {
            i++;
            assertTrue(ps.contains(pathstr));
            assertTrue(ps.contains("/" + pathstr));
            assertTrue(ps.contains("/" + pathstr.replace('/', '\\')));
            assertFalse(ps.contains("/" + pathstr.replace('/', '\\') + "/a"));
            assertFalse(ps.contains("/a/" + pathstr.replace('/', '\\')));
        }
        assertEquals(2, i, "Wrong count of iterations");

        ps.addPrefix("/ab/c/");
        i = 0;
        for (String pathstr : ps) {
            i++;
            assertTrue(pathstr.startsWith("ab/c/"));
            assertFalse(pathstr.startsWith("ab/c//"));
            assertTrue(ps.contains(pathstr));
            assertTrue(ps.contains("/" + pathstr));
            assertTrue(ps.contains("/" + pathstr.replace('/', '\\')));
            assertFalse(ps.contains("/" + pathstr.replace('/', '\\') + "/a"));
            assertFalse(ps.contains("/ab/" + pathstr.replace('/', '\\')));
        }
        assertEquals(2, i, "Wrong count of iterations");
    }

    /**
     * Test method for:
     * <ul>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet(Collection)</li>
     * <li>org.apache.maven.plugin.war.PathSet.PathSet(String[])</li>
     * <li>org.apache.maven.plugin.war.PathSet.Add</li>
     * <li>org.apache.maven.plugin.war.PathSet.AddAll(String[],String)</li>
     * <li>org.apache.maven.plugin.war.PathSet.AddAll(Collection,String)</li>
     * </ul>
     */
    @Test
    public void testPathsSetAddAlls() {
        Set<String> s1set = new HashSet<>();
        s1set.add("/a/b");
        s1set.add("a/b/c");
        s1set.add("a\\b/c");
        s1set.add("//1//2\3a");

        String[] s2ar = new String[] {"/a/b", "a2/b2/c2", "a2\\b2/c2", "//21//22\23a"};

        PathSet ps1 = new PathSet(s1set);
        assertEquals(3, ps1.size(), "Unexpected PathSet size");

        PathSet ps2 = new PathSet(s2ar);
        assertEquals(3, ps2.size(), "Unexpected PathSet size");

        ps1.addAll(s2ar);
        assertEquals(5, ps1.size(), "Unexpected PathSet size");

        ps2.addAll(s1set);
        assertEquals(5, ps2.size(), "Unexpected PathSet size");

        for (String str : ps1) {
            assertTrue(ps2.contains(str), str);
            assertTrue(ps2.contains("/" + str));
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
        }

        for (String str : ps2) {
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
            assertTrue(ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
        }

        ps1.addAll(s2ar, "/pref/");
        assertEquals(8, ps1.size(), "Unexpected PathSet size");

        ps2.addAll(s2ar, "/pref/");
        assertEquals(8, ps2.size(), "Unexpected PathSet size");

        for (String str : ps1) {
            assertTrue(ps2.contains(str), str);
            assertTrue(ps2.contains("/" + str));
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
        }

        for (String str : ps2) {
            assertTrue(ps1.contains(str));
            assertTrue(ps1.contains("/" + str));
            assertTrue(ps2.contains(str));
            assertTrue(ps2.contains("/" + str));
        }

        PathSet ps3 = new PathSet();
        ps3.addAll(new String[] {"a/b/c"}, "d");
        assertTrue(ps3.contains("d/a/b/c"), "Unexpected PathSet path");
    }

    /**
     * Test method for 'org.apache.maven.plugin.war.PathSet.addAllFilesInDirectory(File, String)'
     *
     * @throws IOException if an io error occurred
     */
    @Test
    public void testAddAllFilesInDirectory() throws IOException {
        PathSet ps = new PathSet();

        /* Preparing directory structure*/
        File testDir = new File("target/testAddAllFilesInDirectory");
        testDir.mkdirs();

        File f1 = new File(testDir, "f1");
        f1.createNewFile();
        File f2 = new File(testDir, "f2");
        f2.createNewFile();

        File d1 = new File(testDir, "d1");
        File d1d2 = new File(testDir, "d1/d2");
        d1d2.mkdirs();
        File d1d2f1 = new File(d1d2, "f1");
        d1d2f1.createNewFile();
        File d1d2f2 = new File(d1d2, "f2");
        d1d2f2.createNewFile();

        ps.addAllFilesInDirectory(new File("target/testAddAllFilesInDirectory"), "123/");
        assertEquals(4, ps.size(), "Unexpected PathSet size");

        /*No changes after adding duplicates*/
        ps.addAllFilesInDirectory(new File("target/testAddAllFilesInDirectory"), "123/");
        assertEquals(4, ps.size(), "Unexpected PathSet size");

        /*Cleanup*/

        f1.delete();
        f2.delete();

        /*No changes after adding a subset of files*/
        ps.addAllFilesInDirectory(new File("target/testAddAllFilesInDirectory"), "123/");
        assertEquals(4, ps.size(), "Unexpected PathSet size");

        d1d2f1.delete();
        d1d2f2.delete();
        d1d2.delete();
        d1.delete();
        testDir.delete();

        assertTrue(ps.contains("123/f1"));
        assertTrue(ps.contains("/123/f1"));
        assertTrue(ps.contains("123\\f1"));
        assertTrue(ps.contains("123\\f2"));
        assertTrue(ps.contains("\\123/d1\\d2/f1"));
        assertTrue(ps.contains("123\\d1/d2\\f2"));
        assertFalse(ps.contains("123\\f3"));
    }
}
