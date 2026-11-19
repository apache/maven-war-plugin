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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
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

    private static final String DATA = "target/test-classes/unit/warexplodedmojo/";

    private static String read(File file, String charset) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), charset);
    }

    private static Resource webResource(String testData, String directory, boolean filtering) {
        Resource resource = new Resource();
        resource.setDirectory(MojoExtension.getBasedir() + "/" + testData + "/" + directory + "/");
        resource.setFiltering(filtering);
        return resource;
    }

    /**
     * Starts from an empty webapp directory: the mojo does not overwrite a target file that is newer than its source,
     * so the output of an earlier run would hide a filtering that no longer happens.
     */
    private void prepare(WarExplodedMojo mojo) throws IOException {
        FileUtils.deleteDirectory(mojo.getWebappDirectory());
        when(mavenSession.getSystemProperties()).thenReturn(System.getProperties());
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringDeploymentDescriptors-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringDeploymentDescriptors-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringDeploymentDescriptors")
    @MojoParameter(name = "filteringDeploymentDescriptors", value = "true")
    @Test
    public void testFilteringDeploymentDescriptors(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        mojo.execute();

        File webappDirectory = mojo.getWebappDirectory();
        assertTrue(new File(webappDirectory, "index.jsp").isFile(), "index.jsp not found");
        File webXml = new File(webappDirectory, "WEB-INF/web.xml");
        assertTrue(webXml.isFile(), "web.xml not found");
        assertTrue(
                read(webXml, "UTF-8").contains("<display-name>Test Description</display-name>"),
                "web.xml not filtered");
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR129-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR129-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR129")
    @Test
    public void testFilteringWebResourceFromWarSourceDirectory(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        Resource resource = webResource("FilteringMWAR129-test-data", "source", true);
        resource.setTargetPath(".");
        resource.addInclude("param.jsp");
        mojo.setWebResources(new Resource[] {resource});
        mojo.execute();

        File param = new File(mojo.getWebappDirectory(), "param.jsp");
        assertTrue(param.isFile(), "param.jsp not found");
        assertTrue(
                read(param, "UTF-8").contains("<c:set var=\"app_version\" value=\"0.0-Test\"/>"),
                "param.jsp not filtered");
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR133-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR133-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR133")
    @Test
    public void testFilteringKeepsUnresolvedExpression(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        mojo.setWebResources(new Resource[] {webResource("FilteringMWAR133-test-data", "resources", true)});
        mojo.execute();

        File filtered = new File(mojo.getWebappDirectory(), "filtered.properties");
        assertTrue(filtered.isFile(), "filtered.properties not found");
        String content = read(filtered, "UTF-8");
        assertTrue(content.contains("app.version=${node.version}"), "unresolved expression was replaced: " + content);
        assertTrue(content.contains("app.artifactId=maven-war-plugin-test"), "resolved expression not filtered");
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR415-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR415-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR415")
    @Test
    public void testFilteringWebResourceSubstitutesBuildTimestamp(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        mojo.setWebResources(new Resource[] {webResource("FilteringMWAR415-test-data", "resources", true)});
        mojo.execute();

        File properties = new File(mojo.getWebappDirectory(), "build-timestamp.properties");
        assertTrue(properties.isFile(), "build-timestamp.properties not found");
        String content = read(properties, "UTF-8");
        assertTrue(
                content.contains("build.timestamp=") && !content.contains("${maven.build.timestamp}"),
                "maven.build.timestamp was not substituted: " + content);
        assertTrue(
                content.matches("(?s).*build\\.timestamp=\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z.*"),
                "substituted timestamp does not match the expected format: " + content);
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR139-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR139-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR139")
    @Test
    public void testFilteringKeepsDelimiterLookalikes(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        mojo.setWebResources(new Resource[] {webResource("FilteringMWAR139-test-data", "resources", true)});
        mojo.execute();

        File filterMe = new File(mojo.getWebappDirectory(), "filterme.xml");
        assertTrue(filterMe.isFile(), "filterme.xml not found");
        String content = read(filterMe, "UTF-8");
        assertTrue(content.contains("<text>hallo @@ hallo</text>"), "'@@' was changed: " + content);
        assertTrue(content.contains("<text2>start-${}-end</text2>"), "'${}' was changed: " + content);
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR306-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR306-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR306")
    @MojoParameter(name = "failOnMissingWebXml", value = "false")
    @Test
    public void testFilteringWebResourceSameAsWarSourceDirectory(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        mojo.setWebResources(new Resource[] {webResource("FilteringMWAR306-test-data", "source", true)});
        mojo.execute();

        File indexHtml = new File(mojo.getWebappDirectory(), "index.html");
        assertTrue(indexHtml.isFile(), "index.html not found");
        assertTrue(
                read(indexHtml, "UTF-8").contains("<body>maven-war-plugin-test @ 0.0-Test</body>"),
                "index.html not filtered");
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR311-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR311-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR311")
    @Test
    public void testFilteringWithBuildFiltersAndPropertyWithSpace(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        ((MavenProjectBasicStub) project).addProperty("my.maven.property", "foo foo");
        project.getBuild()
                .addFilter(MojoExtension.getBasedir() + "/FilteringMWAR311-test-data/filters/default.properties");
        mojo.setWebResources(new Resource[] {webResource("FilteringMWAR311-test-data", "resources", true)});
        mojo.execute();

        File properties = new File(mojo.getWebappDirectory(), "app.properties");
        assertTrue(properties.isFile(), "app.properties not found");
        String content = read(properties, "UTF-8");
        assertTrue(content.contains("my.maven.property=foo foo"), "project property not filtered: " + content);
        assertTrue(content.contains("prop.a=AAA"), "build filter prop.a not applied: " + content);
        assertTrue(content.contains("prop.b=BBB"), "build filter prop.b not applied: " + content);
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR450-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR450-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR450")
    @MojoParameter(name = "resourceEncoding", value = "UTF-8")
    @MojoParameter(name = "propertiesEncoding", value = "ISO-8859-1")
    @Test
    public void testFilteringKeepsEncodings(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        Resource resource = webResource("FilteringMWAR450-test-data", "resources", true);
        resource.setTargetPath("WEB-INF/classes");
        mojo.setWebResources(new Resource[] {resource});
        mojo.execute();

        File classes = new File(mojo.getWebappDirectory(), "WEB-INF/classes");
        // a-ring, a-umlaut and o-umlaut
        String aao = "\u00e5\u00e4\u00f6";

        File log4j = new File(classes, "log4j.xml");
        assertTrue(log4j.isFile(), "log4j.xml not found");
        assertTrue(
                read(log4j, "UTF-8")
                        .contains("This file is encoded in UTF-8 and should remain so after filtering - " + aao),
                "UTF-8 characters of log4j.xml changed");

        File myProperties = new File(classes, "my.properties");
        assertTrue(myProperties.isFile(), "my.properties not found");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(myProperties.toPath())) {
            // Properties.load(InputStream) always reads ISO-8859-1
            properties.load(in);
        }
        assertEquals(
                "Characters that should be encoded in ISO-8859-1: " + aao,
                properties.getProperty("my.property"),
                "ISO-8859-1 characters of my.properties changed");

        File myFile = new File(classes, "myfile.txt");
        assertTrue(myFile.isFile(), "myfile.txt not found");
        assertTrue(
                read(myFile, "UTF-8").contains("Characters that should be encoded in UTF-8: " + aao),
                "UTF-8 characters of myfile.txt changed");
    }

    @InjectMojo(goal = "exploded", pom = "plugin-config.xml")
    @Basedir("src/test/resources/unit/warexplodedmojo/")
    @MojoParameter(name = "classesDirectory", value = DATA + "FilteringMWAR96-test-data/classes/")
    @MojoParameter(name = "warSourceDirectory", value = DATA + "FilteringMWAR96-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = DATA + "FilteringMWAR96")
    @MojoParameter(name = "filters", value = DATA + "FilteringMWAR96-test-data/filters/filter.properties")
    @Test
    public void testFilteringWithFilterFileAndSystemProperty(WarExplodedMojo mojo) throws Exception {
        prepare(mojo);
        Resource resource = webResource("FilteringMWAR96-test-data", "source", true);
        resource.addInclude("index.jsp");
        mojo.setWebResources(new Resource[] {resource});
        mojo.execute();

        File indexJsp = new File(mojo.getWebappDirectory(), "index.jsp");
        assertTrue(indexJsp.isFile(), "index.jsp not found");
        String content = read(indexJsp, "UTF-8");
        assertTrue(
                content.contains("<td>java version : " + System.getProperty("java.version") + "</td>"),
                "system property not filtered: " + content);
        assertTrue(content.contains("<td>okitworks</td>"), "filter file property not applied: " + content);
    }
}
