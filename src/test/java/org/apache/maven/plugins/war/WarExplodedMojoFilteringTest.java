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

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
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
    private List<String> filters;

    @Inject
    private MavenSession mavenSession;

    protected File getTestDirectory() {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedmojo/test-dir");
    }

    @Provides
    List<String> filters() {
        List<String> filtersList = new ArrayList<>();
        filtersList.add("test-filter"); //only for demo, it is temporary
        return filtersList;
    }

    @InjectMojo(goal="exploded", pom = "src/test/resources/unit/warexplodedmojo/plugin-config.xml")
    @Test
    public void testExplodedWarWithResourceFiltering(WarExplodedMojo mojo) throws Exception {
        // setup test data
        String testId = "ExplodedWarWithResourceFiltering";
        File webAppDirectory = new File(getTestDirectory(), testId);
        File webAppSource = createWebAppSource(testId);
        File classesDir = createClassesDir(testId, false);
        File webAppResource = new File(getTestDirectory(), testId + "-test-data/resources");
        File sampleResource = new File(webAppResource, "custom-setting.cfg");
        File sampleResourceWDir = new File(webAppResource, "custom-config/custom-setting.cfg");

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

        Properties systemProperties = System.getProperties();
        systemProperties.put("system.property", "system-property-value");
        when(mavenSession.getSystemProperties()).thenReturn(systemProperties);

        // configure mojo
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        project.addProperty("is_this_simple", "i_think_so");
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(webAppResource.getAbsolutePath());
        resources[0].setFiltering(true);
        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);
        mojo.setWebResources(resources);

        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File(webAppDirectory, "pansit.jsp");
        File expectedWebSource2File = new File(webAppDirectory, "org/web/app/last-exile.jsp");
        File expectedResourceFile = new File(webAppDirectory, "custom-setting.cfg");
        File expectedResourceWDirFile = new File(webAppDirectory, "custom-config/custom-setting.cfg");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedResourceFile.exists(), "resource file not found:" + expectedResourceFile);
        assertTrue(
                expectedResourceWDirFile.exists(),
                "resource file with dir not found:" + expectedResourceWDirFile);

        // validate filtered file
        content = FileUtils.fileRead(expectedResourceWDirFile);
        BufferedReader reader = new BufferedReader(new StringReader(content));

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

        configureMojo(mojo, classesDir, webAppSource, webAppDirectory, project);

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
        filters.add(filterFile.getAbsolutePath());
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

        // house keeping
        expectedWebSourceFile.delete();
        expectedWebSource2File.delete();
        expectedResourceFile.delete();
        expectedResourceWDirFile.delete();
    }


    private void configureMojo(WarExplodedMojo mojo, File classesDir, File webAppSource, File webAppDirectory, MavenProjectBasicStub project) {
        mojo.setClassesDirectory(classesDir);
        mojo.setWarSourceDirectory(webAppSource);
        mojo.setWebappDirectory(webAppDirectory);
        mojo.setProject(project);
    }

    /**
     * Returns the webapp source directory for the specified id.
     *
     * @param id the id of the test
     * @return the source directory for that test
     * @throws Exception if an exception occurs
     */
    protected File getWebAppSource(String id) throws Exception {
        return new File(getTestDirectory(), "/" + id + "-test-data/source");
    }

    /**
     * create an isolated web source with a sample jsp file
     *
     * @param id The id.
     * @param createSamples Create example files yes or no.
     * @return The created file.
     * @throws Exception in case of errors.
     */
    protected File createWebAppSource(String id, boolean createSamples) throws Exception {
        File webAppSource = getWebAppSource(id);
        if (createSamples) {
            File simpleJSP = new File(webAppSource, "pansit.jsp");
            File jspFile = new File(webAppSource, "org/web/app/last-exile.jsp");

            createFile(simpleJSP);
            createFile(jspFile);
        }
        return webAppSource;
    }

    protected File createWebAppSource(String id) throws Exception {
        return createWebAppSource(id, true);
    }

    /**
     * create a class directory with or without a sample class
     *
     * @param id The id.
     * @param empty true to create a class files false otherwise.
     * @return The created class file.
     * @throws Exception in case of errors.
     */
    protected File createClassesDir(String id, boolean empty) throws Exception {
        File classesDir = new File(getTestDirectory() + "/" + id + "-test-data/classes/");

        createDir(classesDir);

        if (!empty) {
            createFile(new File(classesDir + "/sample-servlet.clazz"));
        }

        return classesDir;
    }

    protected void createDir(File dir) {
        if (!dir.exists()) {
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir);
        }
    }

    protected void createFile(File testFile, String body) throws Exception {
        createDir(testFile.getParentFile());
        FileUtils.fileWrite(testFile.toString(), body);

        assertTrue(testFile.exists(), "could not create file: " + testFile);
    }

    protected void createFile(File testFile) throws Exception {
        createFile(testFile, testFile.toString());
    }

}
