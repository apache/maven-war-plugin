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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.api.PathScope;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.plugins.war.util.ClassesPackager;
import org.apache.maven.shared.archiver.MavenArchiver;
import org.apache.maven.shared.archiver.MavenArchiverException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;

/**
 * Build a WAR file.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 */
@Mojo(name = "war", defaultPhase = "package")
public class WarMojo extends AbstractWarMojo {
    /**
     * The directory for the generated WAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String outputDirectory;

    /**
     * The name of the generated WAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String warName;

    /**
     * Classifier to add to the generated WAR. If given, the artifact will be an attachment instead. The classifier will
     * not be applied to the JAR file of the project - only to the WAR file.
     */
    @Parameter
    private String classifier;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or deploy
     * it to the local repository instead of the default one in an execution.
     */
    @Parameter(defaultValue = "true")
    private boolean primaryArtifact;

    /**
     * Whether classes (that is the content of the WEB-INF/classes directory) should be attached to the project as an
     * additional artifact.
     * <p>
     * By default the classifier for the additional artifact is 'classes'. You can change it with the
     * <code><![CDATA[<classesClassifier>someclassifier</classesClassifier>]]></code> parameter.
     * </p>
     * <p>
     * If this parameter true, another project can depend on the classes by writing something like:
     *
     * <pre>
     * <![CDATA[<dependency>
     *   <groupId>myGroup</groupId>
     *   <artifactId>myArtifact</artifactId>
     *   <version>myVersion</myVersion>
     *   <classifier>classes</classifier>
     * </dependency>]]>
     * </pre>
     * </p>
     *
     * @since 2.1-alpha-2
     */
    @Parameter(defaultValue = "false")
    private boolean attachClasses;

    /**
     * The classifier to use for the attached classes artifact.
     *
     * @since 2.1-alpha-2
     */
    @Parameter(defaultValue = "classes")
    private String classesClassifier;

    /**
     * You can skip the execution of the plugin if you need to. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 3.0.0
     */
    @Parameter(property = "maven.war.skip", defaultValue = "false")
    private boolean skip;

    @Inject
    private ProjectManager projectManager;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    /**
     * Executes the WarMojo on the current project.
     *
     * @throws MojoException if an error occurred while building the webapp
     */
    @Override
    public void execute() {

        if (isSkip()) {
            getLog().info("Skipping the execution.");
            return;
        }

        File warFile = getTargetWarFile();

        try {
            performPackaging(warFile);
        } catch (MavenArchiverException | ArchiverException e) {
            throw new MojoException("Error assembling WAR: " + e.getMessage(), e);
        } catch (ManifestException | IOException e) {
            throw new MojoException("Error assembling WAR", e);
        }
    }

    /**
     * Generates the webapp according to the {@code mode} attribute.
     *
     * @param warFile the target WAR file
     * @throws ArchiverException if the archive could not be created
     * @throws MavenArchiverException if an error occurred while creating the archive
     * @throws IOException if an error occurred while copying files
     * @throws ManifestException if the manifest could not be created
     * @throws MojoException if the execution failed
     */
    private void performPackaging(File warFile)
            throws IOException, ManifestException, MavenArchiverException, MojoException {
        getLog().info("Packaging webapp");

        buildExplodedWebapp(getWebappDirectory());

        MavenArchiver archiver = new MavenArchiver();

        WarArchiver warArchiver = getWarArchiver();
        archiver.setArchiver(warArchiver);

        archiver.setCreatedBy("Maven WAR Plugin", "org.apache.maven.plugins", "maven-war-plugin");

        archiver.setOutputFile(warFile);

        // configure for Reproducible Builds based on outputTimestamp value
        archiver.configureReproducibleBuild(outputTimestamp);

        getLog().debug("Excluding " + Arrays.asList(getPackagingExcludes()) + " from the generated webapp archive.");
        getLog().debug("Including " + Arrays.asList(getPackagingIncludes()) + " in the generated webapp archive.");

        warArchiver.addDirectory(getWebappDirectory(), getPackagingIncludes(), getPackagingExcludes());

        final File webXmlFile = new File(getWebappDirectory(), "WEB-INF/web.xml");
        if (webXmlFile.exists()) {
            warArchiver.setWebxml(webXmlFile);
        }

        warArchiver.setRecompressAddedZips(isRecompressZippedFiles());

        warArchiver.setIncludeEmptyDirs(isIncludeEmptyDirectories());

        if (Boolean.FALSE.equals(failOnMissingWebXml)
                || (failOnMissingWebXml == null && isProjectUsingAtLeastServlet30())) {
            getLog().debug("Build won't fail if web.xml file is missing.");
            warArchiver.setExpectWebXml(false);
        }

        // create archive
        archiver.createArchive(getSession(), getProject(), getArchive());

        // create the classes to be attached if necessary
        if (isAttachClasses()) {
            if (isArchiveClasses() && getJarArchiver().getDestFile() != null) {
                // special handling in case of archived classes: MWAR-240
                File targetClassesFile = getTargetClassesFile();
                FileUtils.copyFile(getJarArchiver().getDestFile(), targetClassesFile);
                ProducedArtifact classesArtifact = getSession()
                        .createProducedArtifact(
                                getProject().getGroupId(),
                                getProject().getArtifactId(),
                                getProject().getVersion(),
                                getClassesClassifier(),
                                "jar",
                                null);
                projectManager.attachArtifact(getProject(), classesArtifact, targetClassesFile.toPath());
            } else {
                ClassesPackager packager = new ClassesPackager();
                final File classesDirectory = packager.getClassesDirectory(getWebappDirectory());
                if (classesDirectory.exists()) {
                    getLog().info("Packaging classes");
                    packager.packageClasses(
                            classesDirectory,
                            getTargetClassesFile(),
                            getJarArchiver(),
                            getSession(),
                            getProject(),
                            getArchive(),
                            outputTimestamp);
                    ProducedArtifact classesArtifact = getSession()
                            .createProducedArtifact(
                                    getProject().getGroupId(),
                                    getProject().getArtifactId(),
                                    getProject().getVersion(),
                                    getClassesClassifier(),
                                    "jar",
                                    null);
                    projectManager.attachArtifact(
                            getProject(),
                            classesArtifact,
                            getTargetClassesFile().toPath());
                }
            }
        }

        if (this.classifier != null) {
            ProducedArtifact warArtifact = getSession()
                    .createProducedArtifact(
                            getProject().getGroupId(),
                            getProject().getArtifactId(),
                            getProject().getVersion(),
                            this.classifier,
                            "war",
                            null);
            projectManager.attachArtifact(getProject(), warArtifact, warFile.toPath());
        } else {
            ProducedArtifact artifact = getSession()
                    .createProducedArtifact(
                            getProject().getGroupId(),
                            getProject().getArtifactId(),
                            getProject().getVersion(),
                            "war");
            getSession().setArtifactPath(artifact, warFile.toPath());
        }
    }

    /**
     * Determines if the current Maven project being built uses the Servlet 3.0 API (JSR 315)
     * or Jakarta Servlet API.
     * If it does then the <code>web.xml</code> file can be omitted.
     * <p>
     * This is done by checking if the interface <code>javax.servlet.annotation.WebServlet</code>
     * or <code>jakarta.servlet.annotation.WebServlet</code> is in the compile-time
     * dependencies (which includes provided dependencies) of the Maven project.
     *
     * @return <code>true</code> if the project being built depends on Servlet 3.0 API or Jakarta Servlet API,
     *         <code>false</code> otherwise
     */
    private boolean isProjectUsingAtLeastServlet30() {
        List<Path> classpathElements = getSession().resolveDependencies(getProject(), PathScope.MAIN_COMPILE);
        URL[] urls = new URL[classpathElements.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = classpathElements.get(i).toUri().toURL();
            } catch (MalformedURLException e) {
                // skip
            }
        }
        URLClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        try {
            return hasWebServletAnnotationClassInClasspath(loader);
        } finally {
            try {
                loader.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    private static boolean hasWebServletAnnotationClassInClasspath(ClassLoader loader) {
        return hasClassInClasspath(loader, "javax.servlet.annotation.WebServlet")
                || hasClassInClasspath(loader, "jakarta.servlet.annotation.WebServlet");
    }

    private static boolean hasClassInClasspath(ClassLoader loader, String clazz) {
        try {
            Class.forName(clazz, false, loader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * @param basedir the basedir
     * @param finalName the finalName
     * @param classifier the classifier
     * @param type the type
     * @return {@link File}
     */
    protected static File getTargetFile(File basedir, String finalName, String classifier, String type) {
        if (classifier == null) {
            classifier = "";
        } else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(basedir, finalName + classifier + "." + type);
    }

    /**
     * @return the war {@link File}
     */
    protected File getTargetWarFile() {
        return getTargetFile(new File(getOutputDirectory()), getWarName(), getClassifier(), "war");
    }

    /**
     * @return the target class {@link File}
     */
    protected File getTargetClassesFile() {
        return getTargetFile(new File(getOutputDirectory()), getWarName(), getClassesClassifier(), "jar");
    }

    // Getters and Setters

    /**
     * @return {@link #classifier}
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * @param classifier {@link #classifier}
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * @return {@link #outputDirectory}
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @param outputDirectory {@link #outputDirectory}
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return {@link #warName}
     */
    public String getWarName() {
        return warName;
    }

    /**
     * @param warName {@link #warName}
     */
    public void setWarName(String warName) {
        this.warName = warName;
    }

    public WarArchiver getWarArchiver() {
        try {
            return (WarArchiver) getArchiverManager().getArchiver("war");
        } catch (NoSuchArchiverException e) {
            throw new IllegalStateException("Cannot find war archiver", e);
        }
    }

    /**
     * @return {@link #projectManager}
     */
    public ProjectManager getProjectManager() {
        return projectManager;
    }

    /**
     * @return {@link #primaryArtifact}
     */
    public boolean isPrimaryArtifact() {
        return primaryArtifact;
    }

    /**
     * @param primaryArtifact {@link #primaryArtifact}
     */
    public void setPrimaryArtifact(boolean primaryArtifact) {
        this.primaryArtifact = primaryArtifact;
    }

    /**
     * @return {@link #attachClasses}
     */
    public boolean isAttachClasses() {
        return attachClasses;
    }

    /**
     * @param attachClasses {@link #attachClasses}
     */
    public void setAttachClasses(boolean attachClasses) {
        this.attachClasses = attachClasses;
    }

    /**
     * @return {@link #classesClassifier}
     */
    public String getClassesClassifier() {
        return classesClassifier;
    }

    /**
     * @param classesClassifier {@link #classesClassifier}
     */
    public void setClassesClassifier(String classesClassifier) {
        this.classesClassifier = classesClassifier;
    }

    /**
     * @return {@link #failOnMissingWebXml}
     */
    public boolean isFailOnMissingWebXml() {
        return failOnMissingWebXml;
    }

    /**
     * @param failOnMissingWebXml {@link #failOnMissingWebXml}
     */
    public void setFailOnMissingWebXml(boolean failOnMissingWebXml) {
        this.failOnMissingWebXml = failOnMissingWebXml;
    }

    /**
     * Skip the mojo run.
     *
     * @return {@link #skip}
     */
    public boolean isSkip() {
        return skip;
    }
}
