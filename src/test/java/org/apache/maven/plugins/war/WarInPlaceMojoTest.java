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

import java.io.File;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugins.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugins.war.stub.ResourceStub;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
public class WarInPlaceMojoTest {

    private File getTestDirectory() throws Exception {
        return new File(getBasedir(), "target/test-classes/unit/warexplodedinplacemojo/test-dir");
    }

    @InjectMojo(goal = "inplace", pom = "src/test/resources/unit/warexplodedinplacemojo/plugin-config.xml")
    @MojoParameter(
            name = "classesDirectory",
            value = "target/test-classes/unit/warexplodedinplacemojo/SimpleExplodedInplaceWar-test-data/classes/")
    @MojoParameter(
            name = "warSourceDirectory",
            value = "target/test-classes/unit/warexplodedinplacemojo/SimpleExplodedInplaceWar-test-data/source/")
    @MojoParameter(name = "webappDirectory", value = "target/test-classes/unit/warexplodedinplacemojo/SimpleExplodedInplaceWar")
    @Test
    public void testSimpleExplodedInplaceWar(WarInPlaceMojo mojo) throws Exception {
        // configure mojo
        ResourceStub[] resources = new ResourceStub[] {new ResourceStub()};
        resources[0].setDirectory(getBasedir() + "/target/test-classes/unit/warexplodedinplacemojo/SimpleExplodedInplaceWar-test-data/resources");
        mojo.setWebResources(resources);
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        mojo.setProject(project);
        mojo.execute();

        // validate operation
        File webAppSource = mojo.getWarSourceDirectory();
        File expectedWebSourceFile = new File(webAppSource, "pansit.jsp");
        File expectedWebSource2File = new File(webAppSource, "org/web/app/last-exile.jsp");
        File expectedWebResourceFile = new File(webAppSource, "pix/panis_na.jpg");
        File expectedWEBINFDir = new File(webAppSource, "WEB-INF");
        File expectedMETAINFDir = new File(webAppSource, "META-INF");

        assertTrue(expectedWebSourceFile.exists(), "source files not found: " + expectedWebSourceFile);
        assertTrue(expectedWebSource2File.exists(), "source files not found: " + expectedWebSource2File);
        assertTrue(expectedWebResourceFile.exists(), "resources doesn't exist: " + expectedWebResourceFile);
        assertTrue(expectedWEBINFDir.exists(), "WEB-INF not found");
        assertTrue(expectedMETAINFDir.exists(), "META-INF not found");
    }

    /**
     * Returns the webapp source directory for the specified id.
     *
     * @param id the id of the test
     * @return the source directory for that test
     * @throws Exception if an exception occurs
     */
    private File getWebAppSource(String id) throws Exception {
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
    private File createWebAppSource(String id, boolean createSamples) throws Exception {
        File webAppSource = getWebAppSource(id);
        if (createSamples) {
            File simpleJSP = new File(webAppSource, "pansit.jsp");
            File jspFile = new File(webAppSource, "org/web/app/last-exile.jsp");

            createFile(simpleJSP);
            createFile(jspFile);
        }
        return webAppSource;
    }

    private File createWebAppSource(String id) throws Exception {
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
    private File createClassesDir(String id, boolean empty) throws Exception {
        File classesDir = new File(getTestDirectory() + "/" + id + "-test-data/classes/");

        createDir(classesDir);

        if (!empty) {
            createFile(new File(classesDir + "/sample-servlet.clazz"));
        }

        return classesDir;
    }

    private void createDir(File dir) {
        if (!dir.exists()) {
            assertTrue(dir.mkdirs(), "can not create test dir: " + dir);
        }
    }

    private void createFile(File testFile, String body) throws Exception {
        createDir(testFile.getParentFile());
        FileUtils.fileWrite(testFile.toString(), body);

        assertTrue(testFile.exists(), "could not create file: " + testFile);
    }

    private void createFile(File testFile) throws Exception {
        createFile(testFile, testFile.toString());
    }
}
