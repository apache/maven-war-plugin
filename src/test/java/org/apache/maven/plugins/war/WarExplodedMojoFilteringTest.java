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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Properties;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Olivier Lamy
 * @since 21 juil. 2008
 */
@MojoTest
public class WarExplodedMojoFilteringTest {

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession mavenSession;

    @Provides
    MavenProject project() throws Exception {
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        project.addProperty("is_this_simple", "i_think_so");
        return project;
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFiltering-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFiltering-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFiltering")
    @MojoParameter(name = "outdatedCheckPath", value = "WEB-INF/lib/")
    @Test
    public void testExplodedWarWithResourceFiltering(WarExplodedMojo mojo) throws Exception {
        Properties systemProperties = System.getProperties();
        systemProperties.put("system.property", "system-property-value");
        when(mavenSession.getSystemProperties()).thenReturn(systemProperties);

        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(
                MojoExtension.getBasedir() + "/ExplodedWarWithResourceFiltering-test-data/resources/");
        resources[0].setFiltering(true);
        mojo.setWebResources(resources);
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(mojo.getWebappDirectory(), "pansit.jsp");
        File expectedWebSource2File = new File(mojo.getWebappDirectory(), "org/web/app/last-exile.jsp");
        File expectedResourceFile = new File(mojo.getWebappDirectory(), "custom-setting.cfg");
        File expectedResourceWDirFile = new File(mojo.getWebappDirectory(), "custom-config/custom-setting.cfg");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedResourceFile.exists(), "resource file not found:" + expectedResourceFile);
        assertTrue(expectedResourceWDirFile.exists(), "resource file with dir not found:" + expectedResourceWDirFile);

        // validate filtered file
        String content = FileUtils.fileRead(expectedResourceWDirFile);
        BufferedReader reader = new BufferedReader(new StringReader(content));
        final String comment = "# this is comment created by author@somewhere@";
        assertEquals(comment, reader.readLine(), "error in filtering using System Properties");

        String line = reader.readLine();
        System.out.println(" line " + line);
        System.out.println(" need " + System.getProperty("user.dir"));
        assertEquals(
                "system_key_1=" + System.getProperty("user.dir"), line, "error in filtering using System properties");
        line = reader.readLine();
        System.out.println(" line " + line);
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
        systemProperties.put("system.property", "new-system-property-value");
        when(mavenSession.getSystemProperties()).thenReturn(systemProperties);

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
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFileFiltering-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFileFiltering-test-data/source/")
    @MojoParameter(
            name = "webappDirectory",
            value = "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFileFiltering")
    @MojoParameter(name = "outdatedCheckPath", value = "WEB-INF/lib/")
    @MojoParameter(
            name = "filters",
            value =
                    "target/test-classes/unit/warexplodedmojo/ExplodedWarWithResourceFileFiltering-test-data/filters/filter.properties")
    @Test
    public void testExplodedWarWithResourceFileFiltering(WarExplodedMojo mojo) throws Exception {
        Properties systemProperties = System.getProperties();
        systemProperties.put("system.property", "system-property-value");
        when(mavenSession.getSystemProperties()).thenReturn(systemProperties);

        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(
                MojoExtension.getBasedir() + "/ExplodedWarWithResourceFileFiltering-test-data/resources/");
        resources[0].setFiltering(true);
        mojo.setWebResources(resources);

        mojo.execute();

        File expectedResourceWDirFile = new File(mojo.getWebappDirectory(), "custom-config/custom-setting.cfg");
        assertTrue(expectedResourceWDirFile.exists(), "resource file with dir not found:" + expectedResourceWDirFile);

        // validate filtered file
        String content = FileUtils.fileRead(expectedResourceWDirFile);
        content = FileUtils.fileRead(expectedResourceWDirFile);
        BufferedReader reader = new BufferedReader(new StringReader(content));

        final String comment = "# this is comment created by author@somewhere@";
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
                "system_property_1=system-property-value",
                reader.readLine(),
                "error in filtering using System properties");
        assertEquals(
                "system_property_2=system-property-value",
                reader.readLine(),
                "error in filtering using System properties");

        assertEquals("resource_key_1=this_is_filtered", reader.readLine(), "error in filtering using filter files");
        assertEquals("resource_key_2=this_is_filtered", reader.readLine(), "error in filtering using filter files");
    }
}
