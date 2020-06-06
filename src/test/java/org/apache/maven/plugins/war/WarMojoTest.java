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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.stub.JarArtifactStub;
import org.apache.maven.plugins.war.stub.MavenProject4CopyConstructor;
import org.apache.maven.plugins.war.stub.MavenProjectArtifactsStub;
import org.apache.maven.plugins.war.stub.ProjectHelperStub;
import org.apache.maven.plugins.war.stub.WarArtifact4CCStub;
import org.codehaus.plexus.util.IOUtil;

/**
 * comprehensive test on buildExplodedWebApp is done on WarExplodedMojoTest
 */
public class WarMojoTest
    extends AbstractWarMojoTest
{
    WarMojo mojo;

    private static File pomFile = new File( getBasedir(),
                                            "target/test-classes/unit/warmojotest/plugin-config-primary-artifact.xml" );

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/warmojotest" );
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        mojo = (WarMojo) lookupMojo( "war", pomFile );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setup
    }

    public void testSimpleWar()
        throws Exception
    {
        String testId = "SimpleWar";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "WEB-INF/web.xml", "pansit.jsp",
            "org/web/app/last-exile.jsp", "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testSimpleWarPackagingExcludeWithIncludesRegEx()
        throws Exception
    {
        String testId = "SimpleWarPackagingExcludeWithIncludesRegEx";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );
        setVariableValueToObject( mojo, "packagingIncludes", "%regex[(.(?!exile))+]" );
//        setVariableValueToObject( mojo, "packagingIncludes", "%regex" );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "WEB-INF/web.xml", "pansit.jsp",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null,
            mojo.getWebXml().toString(), null, null, null, }, new String[] { "org/web/app/last-exile.jsp" } );
    }

    public void testClassifier()
        throws Exception
    {
        String testId = "Classifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "classifier", "test-classifier" );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple-test-classifier.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "WEB-INF/web.xml", "pansit.jsp",
            "org/web/app/last-exile.jsp", "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testPrimaryArtifact()
        throws Exception
    {
        String testId = "PrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        warArtifact.setFile( new File( "error.war" ) );
        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "WEB-INF/web.xml", "pansit.jsp",
            "org/web/app/last-exile.jsp", "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testNotPrimaryArtifact()
        throws Exception
    {
        // use a different pom
        File pom = new File( getBasedir(), "target/test-classes/unit/warmojotest/not-primary-artifact.xml" );
        mojo = (WarMojo) lookupMojo( "war", pom );

        String testId = "NotPrimaryArtifact";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        ProjectHelperStub projectHelper = new ProjectHelperStub();
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        warArtifact.setFile( new File( "error.war" ) );
        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "projectHelper", projectHelper );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "WEB-INF/web.xml", "pansit.jsp",
            "org/web/app/last-exile.jsp", "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testMetaInfContent()
        throws Exception
    {
        String testId = "SimpleWarWithMetaInfContent";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        // Create the sample config.xml
        final File configFile = new File( webAppSource, "META-INF/config.xml" );
        createFile( configFile, "<config></config>" );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "META-INF/config.xml",
            "WEB-INF/web.xml", "pansit.jsp", "org/web/app/last-exile.jsp",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null, null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testMetaInfContentWithContainerConfig()
        throws Exception
    {
        String testId = "SimpleWarWithContainerConfig";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        // Create the sample config.xml
        final File configFile = new File( webAppSource, "META-INF/config.xml" );
        createFile( configFile, "<config></config>" );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );
        mojo.setContainerConfigXML( configFile );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "META-INF/config.xml",
            "WEB-INF/web.xml", "pansit.jsp", "org/web/app/last-exile.jsp",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
            "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null, null,
            mojo.getWebXml().toString(), null, null, null, null } );
    }

    public void testFailOnMissingWebXmlFalse()
        throws Exception
    {

        String testId = "SimpleWarMissingWebXmlFalse";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setFailOnMissingWebXml( false );
        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple.war" );
        final Map<String, JarEntry> jarContent =
            assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "pansit.jsp",
                "org/web/app/last-exile.jsp", "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" }, new String[] { null, null,
                null, null, null } );

        assertFalse( "web.xml should be missing", jarContent.containsKey( "WEB-INF/web.xml" ) );
    }

    public void testFailOnMissingWebXmlTrue()
        throws Exception
    {

        String testId = "SimpleWarMissingWebXmlTrue";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setFailOnMissingWebXml( true );

        try
        {
            mojo.execute();
            fail( "Building of the war isn't possible because web.xml is missing" );
        }
        catch ( MojoExecutionException e )
        {
            // expected behaviour
        }
    }
    
    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30Used()
        throws Exception
    {
        String testId = "SimpleWarUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );

        final ArtifactHandler artifactHandler = (ArtifactHandler) lookup( ArtifactHandler.ROLE, "jar" );
        JarArtifactStub jarArtifactStub = new JarArtifactStub( getBasedir(), artifactHandler );
        jarArtifactStub.setFile( new File( getBasedir(),
                                           "/target/test-classes/unit/sample_wars/javax.servlet-api-3.0.1.jar" ) );
        jarArtifactStub.setScope( Artifact.SCOPE_PROVIDED );
        project.addArtifact( jarArtifactStub );

        project.setArtifact( warArtifact );
        project.setFile( warArtifact.getFile() );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );

        mojo.execute();

        // validate war file
        File expectedWarFile = new File( outputDir, "simple.war" );
        final Map<String, JarEntry> jarContent =
            assertJarContent( expectedWarFile,
                              new String[] { "META-INF/MANIFEST.MF", "pansit.jsp", "org/web/app/last-exile.jsp",
                                  "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.xml",
                                  "META-INF/maven/org.apache.maven.plugin.test/maven-war-plugin-test/pom.properties" },
                              new String[] { null, null, null, null, null } );

        assertFalse( "web.xml should be missing", jarContent.containsKey( "WEB-INF/web.xml" ) );
    }

    public void testFailOnMissingWebXmlNotSpecifiedAndServlet30NotUsed()
        throws Exception
    {
        String testId = "SimpleWarNotUnderServlet30";
        MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );

        project.setArtifact( warArtifact );
        project.setFile( warArtifact.getFile() );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );

        try
        {
            mojo.execute();
            fail( "Building of the war isn't possible because no 'failOnMissingWebXml' policy was set and the project "
                + "does not depend on Servlet 3.0" );
        }
        catch ( MojoExecutionException e )
        {
            // expected behaviour
        }
    }

    public void testAttachClasses()
        throws Exception
    {
        String testId = "AttachClasses";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );
        mojo.setAttachClasses( true );
        mojo.setClassesClassifier( "classes" );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple-classes.jar" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "sample-servlet.class" },
                          new String[] { null, null } );
    }

    public void testAttachClassesWithCustomClassifier()
        throws Exception
    {
        String testId = "AttachClassesCustomClassifier";
        MavenProject4CopyConstructor project = new MavenProject4CopyConstructor();
        String outputDir = getTestDirectory().getAbsolutePath() + "/" + testId + "-output";
        File webAppDirectory = new File( getTestDirectory(), testId );
        WarArtifact4CCStub warArtifact = new WarArtifact4CCStub( getBasedir() );
        String warName = "simple";
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, false );
        File xmlSource = createXMLConfigDir( testId, new String[] { "web.xml" } );

        project.setArtifact( warArtifact );
        this.configureMojo( mojo, new LinkedList<String>(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "outputDirectory", outputDir );
        setVariableValueToObject( mojo, "warName", warName );
        mojo.setWebXml( new File( xmlSource, "web.xml" ) );
        mojo.setAttachClasses( true );
        mojo.setClassesClassifier( "mystuff" );

        mojo.execute();

        // validate jar file
        File expectedJarFile = new File( outputDir, "simple-mystuff.jar" );
        assertJarContent( expectedJarFile, new String[] { "META-INF/MANIFEST.MF", "sample-servlet.class" },
                          new String[] { null, null } );
    }

    protected Map<String, JarEntry> assertJarContent( final File expectedJarFile, final String[] files,
                                                      final String[] filesContent )
        throws IOException
    {
        return assertJarContent( expectedJarFile, files, filesContent, null );
    }

    protected Map<String, JarEntry> assertJarContent( final File expectedJarFile, final String[] files,
                                                      final String[] filesContent, final String[] mustNotBeInJar )
        throws IOException
    {
        // Sanity check
        assertEquals( "Could not test, files and filesContent length does not match", files.length, filesContent.length );

        assertTrue( "war file not created: " + expectedJarFile.toString(), expectedJarFile.exists() );
        final Map<String, JarEntry> jarContent = new HashMap<>();
        try ( JarFile jarFile = new JarFile( expectedJarFile ) ) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while ( enumeration.hasMoreElements() )
            {
                JarEntry entry = enumeration.nextElement();
                Object previousValue = jarContent.put( entry.getName(), entry );
                assertNull( "Duplicate Entry in Jar File: " + entry.getName(), previousValue );
            }
    
            for ( int i = 0; i < files.length; i++ )
            {
                String file = files[i];
    
                assertTrue( "File[" + file + "] not found in archive", jarContent.containsKey( file ) );
                if ( filesContent[i] != null )
                {
                    assertEquals( "Content of file[" + file + "] does not match", filesContent[i],
                                  IOUtil.toString( jarFile.getInputStream( jarContent.get( file ) ) ) );
                }
            }
            if ( mustNotBeInJar != null )
            {
                for ( String file : mustNotBeInJar )
                {
                    assertFalse( "File[" + file + "]  found in archive", jarContent.containsKey( file ) );
    
                }
            }
            return jarContent;
        }
    }

}
