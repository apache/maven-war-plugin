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
package org.apache.maven.plugins.war;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 * @since 21 juil. 2008
 */
class WarExplodedMojoFilteringTest extends AbstractWarExplodedMojoTest {

    protected File getPomFile() {
        return new File(getBasedir(), "/target/test-classes/unit/warexplodedmojo/plugin-config.xml");
    }

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir");
    }

    /**
     * @throws Exception in case of an error.
     */
    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    void explodedWarWithResourceFiltering() throws Exception {
        // setup test data
        String testId = "ExplodedWarWithResourceFiltering";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppResource = new File(getTestDirectory(), testId + "-test-data/resources");
        File sampleResource = new File(webAppResource, "custom-setting.cfg");
        File sampleResourceWDir = new File(webAppResource, "custom-config/custom-setting.cfg");
        List<String> filterList = new LinkedList<>();
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};

        createFile(sampleResource);
        createFile(sampleResourceWDir);

        String ls = System.getProperty("line.separator");
        final String comment = "# this is comment created by author@somewhere@";
        // prepare web resources
        String content = comment + ls;
        content += "system_key_1=${user.dir}" + ls;
        content += "system_key_2=@user.dir@" + ls;
        content += "project_key_1=${is_this_simple}" + ls;
        content += "project_key_2=@is_this_simple@" + ls;
        content += "project_name_1=${project.name}" + ls;
        content += "project_name_2=@project.name@" + ls;
        content += "system_property_1=${system.property}" + ls;
        content += "system_property_2=@system.property@" + ls;
        FileUtils.fileWrite(sampleResourceWDir.getAbsolutePath(), content);
        FileUtils.fileWrite(sampleResource.getAbsolutePath(), content);

        lookup(MavenSession.class).getSystemProperties().setProperty("system.property", "system-property-value");

        // configure mojo
        project.addProperty("is_this_simple", "i_think_so");
        resources[0].setDirectory(webAppResource.getAbsolutePath());
        resources[0].setFiltering(true);
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        setVariableValueToObject(mojo, "webResources", resources);
        setVariableValueToObject(mojo, "filters", filterList);

        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedResourceFile = new File(webAppDirectory, "custom-setting.cfg");
        File expectedResourceWDirFile = new File(webAppDirectory, "custom-config/custom-setting.cfg");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile.toString());
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File.toString());
        assertTrue(expectedResourceFile.exists(), "resource file not found:" + expectedResourceFile.toString());
        assertTrue(
                expectedResourceWDirFile.exists(),
                "resource file with dir not found:" + expectedResourceWDirFile.toString());

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        BufferedReader reader = new BufferedReader(new StringReader(content));

        assertEquals(comment, reader.readLine(), "error in filtering using System Properties");

        String line = reader.readLine();
        assertEquals(
                "system_key_1=" + System.getProperty("user.dir"), line, "error in filtering using System properties");
        line = reader.readLine();
        assertEquals(
                "system_key_2=" + System.getProperty("user.dir"), line, "error in filtering using System properties");

        assertEquals("project_key_1=i_think_so", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_key_2=i_think_so", reader.readLine(), "error in filtering using project properties");

        assertEquals("project_name_1=Test Project ", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_name_2=Test Project ", reader.readLine(), "error in filtering using project properties");

        assertEquals(
                "system_property_1=system-property-value",
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_property_2=system-property-value",
                reader.readLine(),
                "error in filtering using System properties");

        // update property, and generate again
        lookup(MavenSession.class).getSystemProperties().setProperty("system.property", "new-system-property-value");
        this.configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        reader = new BufferedReader(new StringReader(content));

        assertEquals(comment, reader.readLine(), "error in filtering using System Properties");

        assertEquals(
                "system_key_1=" + System.getProperty("user.dir"),
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_key_2=" + System.getProperty("user.dir"),
                reader.readLine(),
                "error in filtering using System properties");

        assertEquals("project_key_1=i_think_so", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_key_2=i_think_so", reader.readLine(), "error in filtering using project properties");

        assertEquals("project_name_1=Test Project ", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_name_2=Test Project ", reader.readLine(), "error in filtering using project properties");

        assertEquals(
                "system_property_1=new-system-property-value",
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_property_2=new-system-property-value",
                reader.readLine(),
                "error in filtering using System properties");

        // update property, and generate again
        File filterFile = new File(getTestDirectory(), testId + "-test-data/filters/filter.properties");
        createFile(filterFile);
        filterList.add(filterFile.getAbsolutePath());
        content += "resource_key_1=${resource_value_1}\n";
        content += "resource_key_2=@resource_value_2@\n" + content;
        FileUtils.fileWrite(sampleResourceWDir.getAbsolutePath(), content);
        FileUtils.fileWrite(sampleResource.getAbsolutePath(), content);
        String filterContent = "resource_value_1=this_is_filtered\n";
        filterContent += "resource_value_2=this_is_filtered";
        FileUtils.fileWrite(filterFile.getAbsolutePath(), filterContent);

        mojo.execute();

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        reader = new BufferedReader(new StringReader(content));

        assertEquals(comment, reader.readLine(), "error in filtering using System Properties");

        assertEquals(
                "system_key_1=" + System.getProperty("user.dir"),
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_key_2=" + System.getProperty("user.dir"),
                reader.readLine(),
                "error in filtering using System properties");

        assertEquals("project_key_1=i_think_so", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_key_2=i_think_so", reader.readLine(), "error in filtering using project properties");

        assertEquals("project_name_1=Test Project ", reader.readLine(), "error in filtering using project properties");
        assertEquals("project_name_2=Test Project ", reader.readLine(), "error in filtering using project properties");

        assertEquals(
                "system_property_1=new-system-property-value",
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_property_2=new-system-property-value",
                reader.readLine(),
                "error in filtering using System properties");

        assertEquals("resource_key_1=this_is_filtered", reader.readLine(), "error in filtering using filter files");
        assertEquals("resource_key_2=this_is_filtered", reader.readLine(), "error in filtering using filter files");

        // house-keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedResourceFile.delete();
        expectedResourceWDirFile.delete();
    }
}
