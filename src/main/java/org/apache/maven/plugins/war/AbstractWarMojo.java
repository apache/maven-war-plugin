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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.war.overlay.OverlayManager;
import org.apache.maven.plugins.war.packaging.CopyUserManifestTask;
import org.apache.maven.plugins.war.packaging.OverlayPackagingTask;
import org.apache.maven.plugins.war.packaging.WarPackagingContext;
import org.apache.maven.plugins.war.packaging.WarPackagingTask;
import org.apache.maven.plugins.war.packaging.WarProjectPackagingTask;
import org.apache.maven.plugins.war.util.WebappStructure;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.FilterWrapper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Contains common jobs for WAR mojos.
 */
public abstract class AbstractWarMojo extends AbstractMojo {
    private static final String META_INF = "META-INF";

    private static final String WEB_INF = "WEB-INF";
    /**
     * Whether or not to fail the build if the <code>web.xml</code> file is missing. Set to <code>false</code> if you
     * want your WAR built without a <code>web.xml</code> file. This may be useful if you are building an overlay that
     * has no web.xml file.
     * <p>
     * Starting with <b>3.1.0</b>, this property defaults to <code>false</code> if the project depends on the Servlet
     * 3.0 API or newer.
     *
     * @since 2.1-alpha-2
     */
    @Parameter
    protected Boolean failOnMissingWebXml;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The directory containing compiled classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File classesDirectory;

    /**
     * Whether a JAR file will be created for the classes in the webapp. Using this optional configuration parameter
     * will make the compiled classes to be archived into a JAR file in <code>/WEB-INF/lib/</code> and the classes
     * directory will then be excluded from the webapp <code>/WEB-INF/classes/</code>.
     *
     * @since 2.0.1
     */
    @Parameter(defaultValue = "false")
    private boolean archiveClasses;

    /**
     * The encoding to use when copying filtered web resources.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String resourceEncoding;

    /**
     * The character encoding to use when reading and writing filtered properties files.
     * If not specified, it will default to the value of the "resourceEncoding" parameter.
     *
     * @since 3.4.0
     */
    @Parameter
    protected String propertiesEncoding;

    /**
     * The JAR archiver needed for archiving the classes directory into a JAR file under WEB-INF/lib.
     */
    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    /**
     * The directory where the webapp is built.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
    private File webappDirectory;

    /**
     * Single directory for extra files to include in the WAR. This is where you place your JSP files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp", required = true)
    private File warSourceDirectory;

    /**
     * The list of webResources we want to transfer.
     */
    @Parameter
    private Resource[] webResources;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     */
    @Parameter
    private List<String> filters;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the form
     * 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p>
     * <p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     *
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt;/delimiter&gt;
     *   &lt;delimiter&gt;@&lt;/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     *
     * @since 3.0.0
     */
    @Parameter
    private LinkedHashSet<String> delimiters;

    /**
     * Use default delimiters in addition to custom delimiters, if any.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "true")
    private boolean useDefaultDelimiters;

    /**
     * The path to the web.xml file to use.
     */
    @Parameter
    private File webXml;

    /**
     * The path to a configuration file for the servlet container. Note that the file name may be different for
     * different servlet containers. Apache Tomcat uses a configuration file named context.xml. The file will be copied
     * to the META-INF directory.
     */
    @Parameter
    private File containerConfigXML;

    /**
     * Directory to unpack dependent WARs into if needed.
     */
    @Parameter(defaultValue = "${project.build.directory}/war/work", required = true)
    private File workDirectory;

    /**
     * The file name mapping to use when copying libraries and TLDs. If no file mapping is set (default) the files are
     * copied with their standard names.
     *
     * @since 2.1-alpha-1
     */
    @Parameter
    private String outputFileNameMapping;

    /**
     */
    @Component(role = ArtifactFactory.class)
    private ArtifactFactory artifactFactory;

    /**
     * To look up Archiver/UnArchiver implementations.
     */
    @Component(role = ArchiverManager.class)
    private ArchiverManager archiverManager;

    /**
     */
    @Component(role = MavenFileFilter.class, hint = "default")
    private MavenFileFilter mavenFileFilter;

    /**
     */
    @Component(role = MavenResourcesFiltering.class, hint = "default")
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * The comma separated list of tokens to include when copying the content of the warSourceDirectory.
     */
    @Parameter(defaultValue = "**")
    private String warSourceIncludes;

    /**
     * The comma separated list of tokens to exclude when copying the content of the warSourceDirectory.
     */
    @Parameter
    private String warSourceExcludes;

    /**
     * The comma separated list of tokens to include when doing a WAR overlay. Default is
     * {@link org.apache.maven.plugins.war.Overlay#DEFAULT_INCLUDES}
     *
     */
    @Parameter
    private String dependentWarIncludes = StringUtils.join(Overlay.DEFAULT_INCLUDES, ",");

    /**
     * The comma separated list of tokens to exclude when doing a WAR overlay. Default is
     * {@link org.apache.maven.plugins.war.Overlay#DEFAULT_EXCLUDES}
     *
     */
    @Parameter
    private String dependentWarExcludes = StringUtils.join(Overlay.DEFAULT_EXCLUDES, ",");

    /**
     * The overlays to apply. Each &lt;overlay&gt; element may contain:
     * <ul>
     * <li>id (defaults to {@code currentBuild})</li>
     * <li>groupId (if this and artifactId are null, then the current project is treated as its own overlay)</li>
     * <li>artifactId (see above)</li>
     * <li>classifier</li>
     * <li>type</li>
     * <li>includes (a list of string patterns)</li>
     * <li>excludes (a list of string patterns)</li>
     * <li>filtered (defaults to false)</li>
     * <li>skip (defaults to false)</li>
     * <li>targetPath (defaults to root of webapp structure)</li>
     * </ul>
     *
     * @since 2.1-alpha-1
     */
    @Parameter
    private List<Overlay> overlays = new ArrayList<>();

    /**
     * A list of file extensions that should not be filtered. <b>Will be used when filtering webResources and
     * overlays.</b>
     *
     * @since 2.1-alpha-2
     */
    @Parameter
    private List<String> nonFilteredFileExtensions;

    /**
     * @since 2.1-alpha-2
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * To filter deployment descriptors. <b>Disabled by default.</b>
     *
     * @since 2.1-alpha-2
     */
    @Parameter(defaultValue = "false")
    private boolean filteringDeploymentDescriptors;

    /**
     * To escape interpolated values with Windows path <code>c:\foo\bar</code> will be replaced with
     * <code>c:\\foo\\bar</code>.
     *
     * @since 2.1-alpha-2
     */
    @Parameter(defaultValue = "false")
    private boolean escapedBackslashesInFilePath;

    /**
     * Expression preceded with this String won't be interpolated. <code>\${foo}</code> will be replaced with
     * <code>${foo}</code>.
     *
     * @since 2.1-beta-1
     */
    @Parameter
    protected String escapeString;

    /**
     * Indicates if zip archives (jar,zip etc) being added to the war should be compressed again. Compressing again can
     * result in smaller archive size, but gives noticeably longer execution time.
     *
     * @since 2.3
     */
    @Parameter(defaultValue = "true")
    private boolean recompressZippedFiles;

    /**
     * @since 2.4
     */
    @Parameter(defaultValue = "false")
    private boolean includeEmptyDirectories;

    /**
     * Stop searching endToken at the end of line
     *
     * @since 2.4
     */
    @Parameter(defaultValue = "false")
    private boolean supportMultiLineFiltering;

    /**
     * use jvmChmod rather that cli chmod and forking process
     *
     * @since 2.4
     */
    @Parameter(defaultValue = "true")
    private boolean useJvmChmod;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     *
     * @since 3.3.0
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    protected String outputTimestamp;

    /**
     * Path prefix for resources that will be checked against outdated content.
     *
     * Starting with <b>3.3.2</b>, if a value of "/" is specified the entire
     * webappDirectory will be checked, i.e. the "/" signifies "root".
     *
     * @since 3.3.1
     */
    @Parameter(defaultValue = "WEB-INF/lib/")
    private String outdatedCheckPath;

    private final Overlay currentProjectOverlay = Overlay.createInstance();

    /**
     * @return The current overlay.
     */
    public Overlay getCurrentProjectOverlay() {
        return currentProjectOverlay;
    }

    /**
     * Returns a string array of the excludes to be used when copying the content of the WAR source directory.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes() {
        List<String> excludeList = new ArrayList<>();
        if (warSourceExcludes != null && !warSourceExcludes.isEmpty()) {
            excludeList.addAll(Arrays.asList(StringUtils.split(warSourceExcludes, ",")));
        }

        // if webXML is specified, omit the one in the source directory
        if (webXml != null && StringUtils.isNotEmpty(webXml.getName())) {
            excludeList.add("**/" + WEB_INF + "/web.xml");
        }

        // if contextXML is specified, omit the one in the source directory
        if (containerConfigXML != null && StringUtils.isNotEmpty(containerConfigXML.getName())) {
            excludeList.add("**/" + META_INF + "/" + containerConfigXML.getName());
        }

        return excludeList.toArray(new String[excludeList.size()]);
    }

    /**
     * Returns a string array of the includes to be used when assembling/copying the WAR.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes() {
        return StringUtils.split(StringUtils.defaultString(warSourceIncludes), ",");
    }

    /**
     * Returns a string array of the excludes to be used when adding dependent WAR as an overlay onto this WAR.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getDependentWarExcludes() {
        return StringUtils.split(StringUtils.defaultString(dependentWarExcludes), ",");
    }

    /**
     * Returns a string array of the includes to be used when adding dependent WARs as an overlay onto this WAR.
     *
     * @return an array of tokens to include
     */
    protected String[] getDependentWarIncludes() {
        return StringUtils.split(StringUtils.defaultString(dependentWarIncludes), ",");
    }

    /**
     * @param webapplicationDirectory The web application directory.
     * @throws MojoExecutionException In case of failure.
     * @throws MojoFailureException In case of failure.
     */
    public void buildExplodedWebapp(File webapplicationDirectory) throws MojoExecutionException, MojoFailureException {
        webapplicationDirectory.mkdirs();

        try {
            buildWebapp(project, webapplicationDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not build webapp", e);
        }
    }

    /**
     * Builds the webapp for the specified project with the new packaging task thingy.
     * Classes, libraries and tld files are copied to the {@code webappDirectory} during this phase.
     *
     * @param mavenProject the maven project
     * @param webapplicationDirectory the target directory
     * @throws MojoExecutionException if an error occurred while packaging the webapp
     * @throws MojoFailureException if an unexpected error occurred while packaging the webapp
     * @throws IOException if an error occurred while copying the files
     */
    public void buildWebapp(MavenProject mavenProject, File webapplicationDirectory)
            throws MojoExecutionException, MojoFailureException, IOException {

        WebappStructure structure = new WebappStructure(mavenProject.getDependencies());

        // CHECKSTYLE_OFF: LineLength
        final long startTime = System.currentTimeMillis();
        getLog().info("Assembling webapp [" + mavenProject.getArtifactId() + "] in [" + webapplicationDirectory + "]");

        final OverlayManager overlayManager = new OverlayManager(
                overlays, mavenProject, getDependentWarIncludes(), getDependentWarExcludes(), currentProjectOverlay);
        // CHECKSTYLE_ON: LineLength
        List<FilterWrapper> defaultFilterWrappers;
        try {
            MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
            mavenResourcesExecution.setEscapeString(escapeString);
            mavenResourcesExecution.setSupportMultiLineFiltering(supportMultiLineFiltering);
            mavenResourcesExecution.setMavenProject(mavenProject);

            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            mavenResourcesExecution.setDelimiters(delimiters, useDefaultDelimiters);

            if (nonFilteredFileExtensions != null) {
                mavenResourcesExecution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
            }

            if (filters == null) {
                filters = getProject().getBuild().getFilters();
            }
            mavenResourcesExecution.setFilters(filters);
            mavenResourcesExecution.setEscapedBackslashesInFilePath(escapedBackslashesInFilePath);
            mavenResourcesExecution.setMavenSession(this.session);
            mavenResourcesExecution.setEscapeString(this.escapeString);
            mavenResourcesExecution.setSupportMultiLineFiltering(supportMultiLineFiltering);

            defaultFilterWrappers = mavenFileFilter.getDefaultFilterWrappers(mavenResourcesExecution);

        } catch (MavenFilteringException e) {
            getLog().error("fail to build filtering wrappers " + e.getMessage());
            throw new MojoExecutionException(e.getMessage(), e);
        }

        final WarPackagingContext context = new DefaultWarPackagingContext(
                webapplicationDirectory,
                structure,
                overlayManager,
                defaultFilterWrappers,
                getNonFilteredFileExtensions(),
                filteringDeploymentDescriptors,
                this.artifactFactory,
                resourceEncoding,
                propertiesEncoding,
                useJvmChmod,
                failOnMissingWebXml,
                outputTimestamp);

        final List<WarPackagingTask> packagingTasks = getPackagingTasks(overlayManager);

        for (WarPackagingTask warPackagingTask : packagingTasks) {
            warPackagingTask.performPackaging(context);
        }

        getLog().debug("Webapp assembled in [" + (System.currentTimeMillis() - startTime) + " msecs]");
    }

    /**
     * Returns a {@code List} of the {@link org.apache.maven.plugins.war.packaging.WarPackagingTask}
     * instances to invoke to perform the packaging.
     *
     * @param overlayManager the overlay manager
     * @return the list of packaging tasks
     * @throws MojoExecutionException if the packaging tasks could not be built
     */
    private List<WarPackagingTask> getPackagingTasks(OverlayManager overlayManager) throws MojoExecutionException {
        final List<WarPackagingTask> packagingTasks = new ArrayList<>();

        packagingTasks.add(new CopyUserManifestTask());

        final List<Overlay> resolvedOverlays = overlayManager.getOverlays();
        for (Overlay overlay : resolvedOverlays) {
            if (overlay.isCurrentProject()) {
                packagingTasks.add(
                        new WarProjectPackagingTask(webResources, webXml, containerConfigXML, currentProjectOverlay));
            } else {
                packagingTasks.add(new OverlayPackagingTask(overlay, currentProjectOverlay));
            }
        }
        return packagingTasks;
    }

    /**
     * WarPackagingContext default implementation
     */
    private class DefaultWarPackagingContext implements WarPackagingContext {
        private final ArtifactFactory artifactFactory;

        private final String resourceEncoding;

        private final String propertiesEncoding;

        private final WebappStructure webappStructure;

        private final File webappDirectory;

        private final OverlayManager overlayManager;

        private final List<FilterWrapper> filterWrappers;

        private List<String> nonFilteredFileExtensions;

        private boolean filteringDeploymentDescriptors;

        private boolean useJvmChmod;

        private final Boolean failOnMissingWebXml;

        private final Collection<String> outdatedResources;

        private final String outputTimestamp;

        /**
         * @param webappDirectory The web application directory.
         * @param webappStructure The web app structure.
         * @param overlayManager The overlay manager.
         * @param filterWrappers The filter wrappers
         * @param nonFilteredFileExtensions The non filtered file extensions.
         * @param filteringDeploymentDescriptors The filtering deployment descriptors.
         * @param artifactFactory The artifact factory.
         * @param resourceEncoding The resource encoding.
         * @param propertiesEncoding The encoding to use for properties files.
         * @param useJvmChmod use Jvm chmod or not.
         * @param failOnMissingWebXml Flag to check whether we should ignore missing web.xml or not
         * @param outputTimestamp the output timestamp for reproducible archive creation
         */
        DefaultWarPackagingContext(
                final File webappDirectory,
                final WebappStructure webappStructure,
                final OverlayManager overlayManager,
                List<FilterWrapper> filterWrappers,
                List<String> nonFilteredFileExtensions,
                boolean filteringDeploymentDescriptors,
                ArtifactFactory artifactFactory,
                String resourceEncoding,
                String propertiesEncoding,
                boolean useJvmChmod,
                final Boolean failOnMissingWebXml,
                String outputTimestamp) {
            this.webappDirectory = webappDirectory;
            this.webappStructure = webappStructure;
            this.overlayManager = overlayManager;
            this.filterWrappers = filterWrappers;
            this.artifactFactory = artifactFactory;
            this.filteringDeploymentDescriptors = filteringDeploymentDescriptors;
            this.nonFilteredFileExtensions =
                    nonFilteredFileExtensions == null ? Collections.emptyList() : nonFilteredFileExtensions;
            this.resourceEncoding = resourceEncoding;
            this.propertiesEncoding = propertiesEncoding;
            // This is kinda stupid but if we loop over the current overlays and we request the path structure
            // it will register it. This will avoid wrong warning messages in a later phase
            for (String overlayId : overlayManager.getOverlayIds()) {
                webappStructure.getStructure(overlayId);
            }
            this.useJvmChmod = useJvmChmod;
            this.failOnMissingWebXml = failOnMissingWebXml;

            if (!webappDirectory.exists()) {
                outdatedResources = Collections.emptyList();
            } else if (getWarSourceDirectory().toPath().equals(webappDirectory.toPath())) {
                getLog().info("Can't detect outdated resources when running inplace goal");
                outdatedResources = Collections.emptyList();
            } else if (session.getStartTime() == null) {
                // MWAR-439: this should never happen, but has happened in some integration context...
                getLog().warn("Can't detect outdated resources because unexpected session.getStartTime() == null");
                outdatedResources = Collections.emptyList();
            } else {
                outdatedResources = new ArrayList<>();
                try {
                    if ('\\' == File.separatorChar) {
                        if (!checkAllPathsForOutdated()) {
                            outdatedCheckPath = outdatedCheckPath.replace('/', '\\');
                        }
                    }
                    Files.walkFileTree(webappDirectory.toPath(), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toFile().lastModified()
                                    < session.getStartTime().getTime()) {
                                String path = webappDirectory
                                        .toPath()
                                        .relativize(file)
                                        .toString();
                                if (checkAllPathsForOutdated() || path.startsWith(outdatedCheckPath)) {
                                    outdatedResources.add(path);
                                }
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
                } catch (IOException e) {
                    getLog().warn("Can't detect outdated resources", e);
                }
            }
            this.outputTimestamp = outputTimestamp;
        }

        protected boolean checkAllPathsForOutdated() {
            return outdatedCheckPath.equals("/");
        }

        @Override
        public MavenProject getProject() {
            return project;
        }

        @Override
        public File getWebappDirectory() {
            return webappDirectory;
        }

        @Override
        public File getClassesDirectory() {
            return classesDirectory;
        }

        @Override
        public Log getLog() {
            return AbstractWarMojo.this.getLog();
        }

        @Override
        public String getOutputFileNameMapping() {
            return outputFileNameMapping;
        }

        @Override
        public File getWebappSourceDirectory() {
            return warSourceDirectory;
        }

        @Override
        public String[] getWebappSourceIncludes() {
            return getIncludes();
        }

        @Override
        public String[] getWebappSourceExcludes() {
            return getExcludes();
        }

        @Override
        public boolean isWebappSourceIncludeEmptyDirectories() {
            return includeEmptyDirectories;
        }

        @Override
        public boolean archiveClasses() {
            return archiveClasses;
        }

        @Override
        public File getOverlaysWorkDirectory() {
            return workDirectory;
        }

        @Override
        public ArchiverManager getArchiverManager() {
            return archiverManager;
        }

        @Override
        public MavenArchiveConfiguration getArchive() {
            return archive;
        }

        @Override
        public JarArchiver getJarArchiver() {
            return jarArchiver;
        }

        @Override
        public List<String> getFilters() {
            return filters;
        }

        @Override
        public WebappStructure getWebappStructure() {
            return webappStructure;
        }

        @Override
        public List<String> getOwnerIds() {
            return overlayManager.getOverlayIds();
        }

        @Override
        public MavenFileFilter getMavenFileFilter() {
            return mavenFileFilter;
        }

        @Override
        public List<FilterWrapper> getFilterWrappers() {
            return filterWrappers;
        }

        @Override
        public boolean isNonFilteredExtension(String fileName) {
            return !mavenResourcesFiltering.filteredFileExtension(fileName, nonFilteredFileExtensions);
        }

        @Override
        public boolean isFilteringDeploymentDescriptors() {
            return filteringDeploymentDescriptors;
        }

        @Override
        public ArtifactFactory getArtifactFactory() {
            return this.artifactFactory;
        }

        @Override
        public MavenSession getSession() {
            return session;
        }

        @Override
        public String getResourceEncoding() {
            return resourceEncoding;
        }

        @Override
        public String getPropertiesEncoding() {
            return propertiesEncoding;
        }

        @Override
        public boolean isUseJvmChmod() {
            return useJvmChmod;
        }

        @Override
        public Boolean isFailOnMissingWebXml() {
            return failOnMissingWebXml;
        }

        @Override
        public void addResource(String resource) {
            outdatedResources.remove(resource.replace('/', File.separatorChar));
        }

        @Override
        public void deleteOutdatedResources() {
            for (String resource : outdatedResources) {
                getLog().info("deleting outdated resource " + resource);
                new File(getWebappDirectory(), resource).delete();
            }
        }

        @Override
        public String getOutputTimestamp() {
            return outputTimestamp;
        }
    }

    /**
     * @return The Maven Project.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * @param project The project to be set.
     */
    public void setProject(MavenProject project) {
        this.project = project;
    }

    /**
     * @return the classes directory.
     */
    public File getClassesDirectory() {
        return classesDirectory;
    }

    /**
     * @param classesDirectory The classes directory to be set.
     */
    public void setClassesDirectory(File classesDirectory) {
        this.classesDirectory = classesDirectory;
    }

    /**
     * @return {@link #webappDirectory}
     */
    public File getWebappDirectory() {
        return webappDirectory;
    }

    /**
     * @param webappDirectory The web application directory.
     */
    public void setWebappDirectory(File webappDirectory) {
        this.webappDirectory = webappDirectory;
    }

    /**
     * @return {@link #warSourceDirectory}
     */
    public File getWarSourceDirectory() {
        return warSourceDirectory;
    }

    /**
     * @param warSourceDirectory {@link #warSourceDirectory}
     */
    public void setWarSourceDirectory(File warSourceDirectory) {
        this.warSourceDirectory = warSourceDirectory;
    }

    /**
     * @return The {@link #webXml}
     */
    public File getWebXml() {
        return webXml;
    }

    /**
     * @param webXml The {@link #webXml}
     */
    public void setWebXml(File webXml) {
        this.webXml = webXml;
    }

    /**
     * @return {@link #containerConfigXML}
     */
    public File getContainerConfigXML() {
        return containerConfigXML;
    }

    /**
     * @param containerConfigXML {@link #containerConfigXML}
     */
    public void setContainerConfigXML(File containerConfigXML) {
        this.containerConfigXML = containerConfigXML;
    }

    /**
     * @return {@link #outputFileNameMapping}
     */
    public String getOutputFileNameMapping() {
        return outputFileNameMapping;
    }

    /**
     * @param outputFileNameMapping {@link #outputFileNameMapping}
     */
    public void setOutputFileNameMapping(String outputFileNameMapping) {
        this.outputFileNameMapping = outputFileNameMapping;
    }

    /**
     * @return {@link #overlays}
     */
    public List<Overlay> getOverlays() {
        return overlays;
    }

    /**
     * @param overlays {@link #overlays}
     */
    public void setOverlays(List<Overlay> overlays) {
        this.overlays = overlays;
    }

    /**
     * @param overlay add {@link #overlays}.
     */
    public void addOverlay(Overlay overlay) {
        overlays.add(overlay);
    }

    /**
     * @return {@link #archiveClasses}
     */
    public boolean isArchiveClasses() {
        return archiveClasses;
    }

    /**
     * @param archiveClasses {@link #archiveClasses}
     */
    public void setArchiveClasses(boolean archiveClasses) {
        this.archiveClasses = archiveClasses;
    }

    /**
     * @return {@link JarArchiver}
     */
    public JarArchiver getJarArchiver() {
        return jarArchiver;
    }

    /**
     * @param jarArchiver {@link JarArchiver}
     */
    public void setJarArchiver(JarArchiver jarArchiver) {
        this.jarArchiver = jarArchiver;
    }

    /**
     * @return {@link #webResources}.
     */
    public Resource[] getWebResources() {
        return webResources;
    }

    /**
     * @param webResources {@link #webResources}.
     */
    public void setWebResources(Resource[] webResources) {
        this.webResources = webResources;
    }

    /**
     * @return {@link #filters}
     */
    public List<String> getFilters() {
        return filters;
    }

    /**
     * @param filters {@link #filters}
     */
    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    /**
     * @return {@link #workDirectory}
     */
    public File getWorkDirectory() {
        return workDirectory;
    }

    /**
     * @param workDirectory {@link #workDirectory}
     */
    public void setWorkDirectory(File workDirectory) {
        this.workDirectory = workDirectory;
    }

    /**
     * @return {@link #warSourceIncludes}
     */
    public String getWarSourceIncludes() {
        return warSourceIncludes;
    }

    /**
     * @param warSourceIncludes {@link #warSourceIncludes}
     */
    public void setWarSourceIncludes(String warSourceIncludes) {
        this.warSourceIncludes = warSourceIncludes;
    }

    /**
     * @return {@link #warSourceExcludes}
     */
    public String getWarSourceExcludes() {
        return warSourceExcludes;
    }

    /**
     * @param warSourceExcludes {@link #warSourceExcludes}
     */
    public void setWarSourceExcludes(String warSourceExcludes) {
        this.warSourceExcludes = warSourceExcludes;
    }

    /**
     * @return {@link #archive}
     */
    public MavenArchiveConfiguration getArchive() {
        return archive;
    }

    /**
     * @return {@link #nonFilteredFileExtensions}
     */
    public List<String> getNonFilteredFileExtensions() {
        return nonFilteredFileExtensions;
    }

    /**
     * @param nonFilteredFileExtensions {@link #nonFilteredFileExtensions}
     */
    public void setNonFilteredFileExtensions(List<String> nonFilteredFileExtensions) {
        this.nonFilteredFileExtensions = nonFilteredFileExtensions;
    }

    /**
     * @return {@link #artifactFactory}
     */
    public ArtifactFactory getArtifactFactory() {
        return this.artifactFactory;
    }

    /**
     * @param artifactFactory {@link #artifactFactory}
     */
    public void setArtifactFactory(ArtifactFactory artifactFactory) {
        this.artifactFactory = artifactFactory;
    }

    /**
     * @return {@link #session}
     */
    protected MavenSession getSession() {
        return this.session;
    }

    /**
     * @return {@link #recompressZippedFiles}
     */
    protected boolean isRecompressZippedFiles() {
        return recompressZippedFiles;
    }

    /**
     * @return {@link #includeEmptyDirectories}
     */
    protected boolean isIncludeEmptyDirectories() {
        return includeEmptyDirectories;
    }
}
