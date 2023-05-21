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
package org.apache.maven.plugins.war.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.war.Overlay;
import org.apache.maven.plugins.war.util.PathSet;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handles the project own resources, that is:
 * <ul>
 * <li>The list of web resources, if any</li>
 * <li>The content of the webapp directory if it exists</li>
 * <li>The custom deployment descriptor(s), if any</li>
 * <li>The content of the classes directory if it exists</li>
 * <li>The dependencies of the project</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
public class WarProjectPackagingTask extends AbstractWarPackagingTask {
    private final Resource[] webResources;

    private final File webXml;

    private final File containerConfigXML;

    private final String id;

    private Overlay currentProjectOverlay;

    /**
     * @param webResources {@link #webResources}
     * @param webXml {@link #webXml}
     * @param containerConfigXml {@link #containerConfigXML}
     * @param currentProjectOverlay {@link #currentProjectOverlay}
     */
    public WarProjectPackagingTask(
            Resource[] webResources, File webXml, File containerConfigXml, Overlay currentProjectOverlay) {
        if (webResources != null) {
            this.webResources = webResources;
        } else {
            this.webResources = new Resource[0];
        }
        this.webXml = webXml;
        this.containerConfigXML = containerConfigXml;
        this.currentProjectOverlay = currentProjectOverlay;
        this.id = currentProjectOverlay.getId();
    }

    @Override
    public void performPackaging(WarPackagingContext context) throws MojoExecutionException, MojoFailureException {
        context.getLog().info("Processing war project");

        // Prepare the INF directories
        File webinfDir = new File(context.getWebappDirectory(), WEB_INF_PATH);
        webinfDir.mkdirs();
        File metainfDir = new File(context.getWebappDirectory(), META_INF_PATH);
        metainfDir.mkdirs();

        handleWebResources(context);

        handleWebAppSourceDirectory(context);

        // Debug mode: dump the path set for the current build
        PathSet pathSet = context.getWebappStructure().getStructure("currentBuild");
        context.getLog().debug("Dump of the current build pathSet content -->");
        for (String path : pathSet) {
            context.getLog().debug(path);
        }
        context.getLog().debug("-- end of dump --");

        handleDeploymentDescriptors(context, webinfDir, metainfDir, context.isFailOnMissingWebXml());

        handleClassesDirectory(context);

        handleArtifacts(context);

        if (!context.getWebappDirectory().mkdirs()) {
            context.deleteOutdatedResources();
        }
    }

    /**
     * Handles the web resources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if a resource could not be copied
     */
    protected void handleWebResources(WarPackagingContext context) throws MojoExecutionException {
        for (Resource resource : webResources) {

            // MWAR-246
            if (resource.getDirectory() == null) {
                throw new MojoExecutionException("The <directory> tag is missing from the <resource> tag.");
            }

            if (!(new File(resource.getDirectory())).isAbsolute()) {
                resource.setDirectory(context.getProject().getBasedir() + File.separator + resource.getDirectory());
            }

            // Make sure that the resource directory is not the same as the webappDirectory
            if (!resource.getDirectory().equals(context.getWebappDirectory().getPath())) {

                try {
                    copyResources(context, resource);
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not copy resource [" + resource.getDirectory() + "]", e);
                }
            }
        }
    }

    /**
     * Handles the webapp sources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the sources could not be copied
     */
    protected void handleWebAppSourceDirectory(WarPackagingContext context) throws MojoExecutionException {
        // CHECKSTYLE_OFF: LineLength
        if (!context.getWebappSourceDirectory().exists()) {
            context.getLog().debug("webapp sources directory does not exist - skipping.");
        } else if (!context.getWebappSourceDirectory()
                .getAbsolutePath()
                .equals(context.getWebappDirectory().getPath())) {
            context.getLog().info("Copying webapp resources [" + context.getWebappSourceDirectory() + "]");
            final PathSet sources = getFilesToIncludes(
                    context.getWebappSourceDirectory(), context.getWebappSourceIncludes(),
                    context.getWebappSourceExcludes(), context.isWebappSourceIncludeEmptyDirectories());

            try {
                copyFiles(id, context, context.getWebappSourceDirectory(), sources, false);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Could not copy webapp sources ["
                                + context.getWebappDirectory().getAbsolutePath() + "]",
                        e);
            }
        }
        // CHECKSTYLE_ON: LineLength
    }

    /**
     * Handles the webapp artifacts.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the artifacts could not be packaged
     */
    protected void handleArtifacts(WarPackagingContext context) throws MojoExecutionException {
        ArtifactsPackagingTask task =
                new ArtifactsPackagingTask(context.getProject().getArtifacts(), currentProjectOverlay);
        task.performPackaging(context);
    }

    /**
     * Handles the webapp classes.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the classes could not be packaged
     */
    protected void handleClassesDirectory(WarPackagingContext context) throws MojoExecutionException {
        ClassesPackagingTask task = new ClassesPackagingTask(currentProjectOverlay);
        task.performPackaging(context);
    }

    /**
     * Handles the deployment descriptors, if specified. Note that the behavior here is slightly different since the
     * customized entry always win, even if an overlay has already packaged a web.xml previously.
     *
     * @param context the packaging context
     * @param webinfDir the web-inf directory
     * @param metainfDir the meta-inf directory
     * @param failOnMissingWebXml if build should fail if web.xml is not found
     * @throws MojoFailureException if the web.xml is specified but does not exist and failOnMissingWebXml is true
     * @throws MojoExecutionException if an error occurred while copying the descriptors
     */
    protected void handleDeploymentDescriptors(
            WarPackagingContext context, File webinfDir, File metainfDir, Boolean failOnMissingWebXml)
            throws MojoFailureException, MojoExecutionException {
        try {
            if (webXml != null && StringUtils.isNotEmpty(webXml.getName())) {
                if (!webXml.exists() && (failOnMissingWebXml == null || Boolean.TRUE.equals(failOnMissingWebXml))) {
                    throw new MojoFailureException("The specified web.xml file '" + webXml + "' does not exist");
                }

                // Making sure that it won't get overlayed
                context.getWebappStructure().registerFileForced(id, WEB_INF_PATH + "/web.xml");

                if (context.isFilteringDeploymentDescriptors()) {
                    context.getMavenFileFilter()
                            .copyFile(
                                    webXml,
                                    new File(webinfDir, "web.xml"),
                                    true,
                                    context.getFilterWrappers(),
                                    getEncoding(webXml));
                } else {
                    copyFile(context, webXml, new File(webinfDir, "web.xml"), "WEB-INF/web.xml", true);
                }
            } else {
                // the webXml can be the default one
                File defaultWebXml = new File(context.getWebappSourceDirectory(), WEB_INF_PATH + "/web.xml");
                // if exists we can filter it
                if (defaultWebXml.exists() && context.isFilteringDeploymentDescriptors()) {
                    context.getWebappStructure().registerFile(id, WEB_INF_PATH + "/web.xml");
                    context.getMavenFileFilter()
                            .copyFile(
                                    defaultWebXml,
                                    new File(webinfDir, "web.xml"),
                                    true,
                                    context.getFilterWrappers(),
                                    getEncoding(defaultWebXml));
                }
            }

            if (containerConfigXML != null && StringUtils.isNotEmpty(containerConfigXML.getName())) {
                String xmlFileName = containerConfigXML.getName();

                context.getWebappStructure().registerFileForced(id, META_INF_PATH + "/" + xmlFileName);

                if (context.isFilteringDeploymentDescriptors()) {
                    context.getMavenFileFilter()
                            .copyFile(
                                    containerConfigXML,
                                    new File(metainfDir, xmlFileName),
                                    true,
                                    context.getFilterWrappers(),
                                    getEncoding(containerConfigXML));
                } else {
                    copyFile(
                            context,
                            containerConfigXML,
                            new File(metainfDir, xmlFileName),
                            "META-INF/" + xmlFileName,
                            true);
                }
            }
        } catch (IOException e) {
            if (failOnMissingWebXml == null || Boolean.TRUE.equals(failOnMissingWebXml)) {
                throw new MojoExecutionException("Failed to copy deployment descriptor", e);
            }
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Failed to copy deployment descriptor", e);
        }
    }

    /**
     * Copies webapp webResources from the specified directory.
     *
     * @param context the WAR packaging context to use
     * @param resource the resource to copy
     * @throws IOException if an error occurred while copying the resources
     * @throws MojoExecutionException if an error occurred while retrieving the filter properties
     */
    public void copyResources(WarPackagingContext context, Resource resource)
            throws IOException, MojoExecutionException {
        if (!context.getWebappDirectory().exists()) {
            context.getLog()
                    .warn("Not copying webapp webResources [" + resource.getDirectory()
                            + "]: webapp directory ["
                            + context.getWebappDirectory().getAbsolutePath()
                            + "] does not exist!");
        }

        context.getLog()
                .info("Copying webapp webResources [" + resource.getDirectory() + "] to ["
                        + context.getWebappDirectory().getAbsolutePath() + "]");
        String[] fileNames = getFilesToCopy(resource);
        for (String fileName : fileNames) {
            String targetFileName = fileName;
            if (resource.getTargetPath() != null) {
                // TODO make sure this thing is 100% safe
                // MWAR-129 if targetPath is only a dot <targetPath>.</targetPath> or ./
                // and the Resource is in a part of the warSourceDirectory the file from sources will override this
                // that's we don't have to add the targetPath yep not nice but works
                if (!Objects.equals(".", resource.getTargetPath()) && !Objects.equals("./", resource.getTargetPath())) {
                    targetFileName = resource.getTargetPath() + File.separator + targetFileName;
                }
            }
            if (resource.isFiltering() && !context.isNonFilteredExtension(fileName)) {
                copyFilteredFile(id, context, new File(resource.getDirectory(), fileName), targetFileName);
            } else {
                copyFile(id, context, new File(resource.getDirectory(), fileName), targetFileName);
            }
        }
    }

    /**
     * Returns a list of filenames that should be copied over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getFilesToCopy(Resource resource) {
        // CHECKSTYLE_OFF: LineLength
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resource.getDirectory());
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            scanner.setIncludes(resource.getIncludes()
                    .toArray(new String[resource.getIncludes().size()]));
        } else {
            scanner.setIncludes(DEFAULT_INCLUDES);
        }
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            scanner.setExcludes(resource.getExcludes()
                    .toArray(new String[resource.getExcludes().size()]));
        }

        scanner.addDefaultExcludes();

        scanner.scan();

        return scanner.getIncludedFiles();
        // CHECKSTYLE_ON: LineLength
    }
}
