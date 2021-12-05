package org.apache.maven.plugins.war.util;

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

import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.components.io.attributes.FileAttributes;
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.ResourceFactory;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.plexus.util.NioFiles;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Filter resources for resources collected as SourceTargetMappings
 */
public class SourceTargetMappingResourceFilter
{

    private WarArchiver warArchiver;

    public SourceTargetMappingResourceFilter( WarArchiver archiver )
    {
        this.warArchiver = archiver;
    }

    public Map<String, PlexusIoResource> filteredResources( String[] includes, String[] excludes, String prefix,
                                                            Map<String, File> mappings ) throws IOException
    {

        // The PlexusIoFileResourceCollection contains platform-specific File.separatorChar which
        // is an interesting cause of grief, see PLXCOMP-192
        final ResourceCollection collection =
                new ResourceCollection( mappings, warArchiver.getFilenameComparator() );
        collection.setFollowingSymLinks( false );

        collection.setIncludes( includes );
        collection.setExcludes( excludes );
        collection.setIncludingEmptyDirectories( warArchiver.getIncludeEmptyDirs() );
        collection.setPrefix( prefix );
        collection.setCaseSensitive( true );
        collection.setUsingDefaultExcludes( true );

        if ( warArchiver.getOverrideDirectoryMode() > -1 || warArchiver.getOverrideFileMode() > -1
                || warArchiver.getOverrideUid() > -1
                || warArchiver.getOverrideGid() > -1 || warArchiver.getOverrideUserName() != null
                || warArchiver.getOverrideGroupName() != null )
        {
            collection.setOverrideAttributes( warArchiver.getOverrideUid(), warArchiver.getOverrideUserName(),
                    warArchiver.getOverrideGid(),
                    warArchiver.getOverrideGroupName(), warArchiver.getOverrideFileMode(),
                    warArchiver.getOverrideDirectoryMode() );
        }

        if ( warArchiver.getDefaultDirectoryMode() > -1 || warArchiver.getDefaultFileMode() > -1 )
        {
            collection.setDefaultAttributes( -1, null, -1, null, warArchiver.getDefaultFileMode(),
                    warArchiver.getDefaultDirectoryMode() );
        }

        return collection.getResourceMap();
    }


    private static class ResourceCollection extends PlexusIoFileResourceCollection
    {

        private Map<String, File> sourceTargetMappings;
        private Comparator<String> fileNameComparator;

        ResourceCollection( Map<String, File> mappings, Comparator<String> fileNameComparator )
        {
            this.sourceTargetMappings = mappings;
            this.fileNameComparator = fileNameComparator;
        }

        public Map<String, PlexusIoResource> getResourceMap() throws IOException
        {
            final SourceTargetMappingResourcesScanner ds = new SourceTargetMappingResourcesScanner();
            ds.setMappings( this.sourceTargetMappings );
            final String[] inc = getIncludes();
            if ( inc != null && inc.length > 0 )
            {
                ds.setIncludes( inc );
            }
            final String[] exc = getExcludes();
            if ( exc != null && exc.length > 0 )
            {
                ds.setExcludes( exc );
            }
            if ( isUsingDefaultExcludes() )
            {
                ds.addDefaultExcludes();
            }
            ds.setCaseSensitive( isCaseSensitive() );
            ds.setFollowSymlinks( isFollowingSymLinks() );
            ds.setFilenameComparator( fileNameComparator );
            ds.scan();

            final Map<String, PlexusIoResource> result = new HashMap<>();
            if ( isIncludingEmptyDirectories() )
            {
                String[] dirs = ds.getIncludedDirectories();
                addResources( result, dirs );
            }

            String[] files = ds.getIncludedFiles();
            addResources( result, files );
            return result;
        }

        private void addResources( Map<String, PlexusIoResource> result, String[] resources )
                throws IOException
        {

            final HashMap<Integer, String> cache1 = new HashMap<>();
            final HashMap<Integer, String> cache2 = new HashMap<>();
            for ( String name : resources )
            {
                File f = sourceTargetMappings.get( name );
                if ( f != null )
                {
                    PlexusIoResourceAttributes attrs = new FileAttributes( f, cache1, cache2 );
                    attrs = mergeAttributes( attrs, f.isDirectory() );

                    String remappedName = getName( name );

                    PlexusIoResource resource =
                            ResourceFactory.createResource( f, remappedName, null, getStreamTransformer(), attrs );

                    if ( isSelected( resource ) )
                    {
                        result.put( name, resource );
                    }
                }

            }
        }
    }


    /**
     * A revised version
     * <p>
     * filter with target path, but mapping file from source path
     */
    private static class SourceTargetMappingResourcesScanner
            extends AbstractScanner
    {


        /**
         * The files which matched at least one include and no excludes and were selected.
         */
        protected Vector<String> filesIncluded;

        /**
         * The files which did not match any includes or selectors.
         */
        protected Vector<String> filesNotIncluded;

        /**
         * The files which matched at least one include and at least one exclude.
         */
        protected Vector<String> filesExcluded;

        /**
         * The directories which matched at least one include and no excludes and were selected.
         */
        protected Vector<String> dirsIncluded;

        /**
         * The directories which were found and did not match any includes.
         */
        protected Vector<String> dirsNotIncluded;

        /**
         * The directories which matched at least one include and at least one exclude.
         */
        protected Vector<String> dirsExcluded;

        /**
         * The files which matched at least one include and no excludes and which a selector discarded.
         */
        protected Vector<String> filesDeselected;

        /**
         * The directories which matched at least one include and no excludes but which a selector discarded.
         */
        protected Vector<String> dirsDeselected;

        /**
         * Whether or not symbolic links should be followed.
         *
         * @since Ant 1.5
         */
        private boolean followSymlinks = true;

        /**
         * Whether or not everything tested so far has been included.
         */
        protected boolean everythingIncluded = true;

        private final String[] tokenizedEmpty = tokenizePathToString( "", File.separator );

        private Map<String, File> sourceTargetMappings = new HashMap<>();

        public void setMappings( Map<String, File> mappings )
        {
            this.sourceTargetMappings = mappings;
        }

        /**
         * Sole constructor.
         */
        SourceTargetMappingResourcesScanner()
        {
        }


        /**
         * Sets whether or not symbolic links should be followed.
         *
         * @param followSymlinks whether or not symbolic links should be followed
         */
        public void setFollowSymlinks( boolean followSymlinks )
        {
            this.followSymlinks = followSymlinks;
        }

        /**
         * Scans the base directory for files which match at least one include pattern and don't match any exclude
         * patterns. If there are selectors then the files must pass muster there, as well.
         *
         * @throws IllegalStateException if the base directory was set incorrectly (i.e. if it is <code>null</code>,
         *                               doesn't exist, or isn't a directory).
         */
        public void scan()
                throws IllegalStateException
        {

            setupDefaultFilters();
            setupMatchPatterns();

            filesIncluded = new Vector<String>();
            filesNotIncluded = new Vector<String>();
            filesExcluded = new Vector<String>();
            filesDeselected = new Vector<String>();
            dirsIncluded = new Vector<String>();
            dirsNotIncluded = new Vector<String>();
            dirsExcluded = new Vector<String>();
            dirsDeselected = new Vector<String>();

            if ( isIncluded( "", tokenizedEmpty ) )
            {

                if ( !isExcluded( "", tokenizedEmpty ) )
                {
                    dirsIncluded.addElement( "" );
                }
                else
                {
                    dirsExcluded.addElement( "" );
                }
            }
            else
            {
                dirsNotIncluded.addElement( "" );
            }
            scanAllFiles( "", true );
        }


        /**
         * Scans the given directory for files and directories. Found files and directories are placed in their
         * respective collections, based on the matching of includes, excludes, and the selectors. When a directory is
         * found, it is scanned recursively.
         *
         * @param vpath The path relative to the base directory (needed to prevent problems with an absolute path when
         *              using dir). Must not be <code>null</code>.
         * @param fast  Whether or not this call is part of a fast scan.
         * @see #filesIncluded
         * @see #filesNotIncluded
         * @see #filesExcluded
         * @see #dirsIncluded
         * @see #dirsNotIncluded
         * @see #dirsExcluded
         */
        protected void scanAllFiles( String vpath, boolean fast )
        {
            String[] newfiles = sourceTargetMappings.keySet().toArray( new String[0] );
            if ( newfiles == null )
            {
                newfiles = new String[0];
            }

            if ( !followSymlinks )
            {
                ArrayList<String> noLinks = new ArrayList<String>();
                for ( String newfile : newfiles )
                {
                    try
                    {
                        File sourceFile = sourceTargetMappings.get( newfile );
                        File dir = sourceFile.getParentFile();
                        if ( isParentSymbolicLink( dir, newfile ) )
                        {
                            String name = vpath + newfile;
                            if ( sourceFile.isDirectory() )
                            {
                                dirsExcluded.addElement( name );
                            }
                            else
                            {
                                filesExcluded.addElement( name );
                            }
                        }
                        else
                        {
                            noLinks.add( newfile );
                        }
                    }
                    catch ( IOException ioe )
                    {
                        String msg = "IOException caught while checking " + "for links, couldn't get canonical path!";
                        // will be caught and redirected to Ant's logging system
                        System.err.println( msg );
                        noLinks.add( newfile );
                    }
                }
                newfiles = noLinks.toArray( new String[noLinks.size()] );
            }

            if ( filenameComparator != null )
            {
                Arrays.sort( newfiles, filenameComparator );
            }

            for ( String newfile : newfiles )
            {
                File file = sourceTargetMappings.get( newfile );
                String name = vpath + newfile;
                String[] tokenizedName = tokenizePathToString( name, "/" );
                if ( file.isFile() )
                {
                    if ( isIncluded( name, tokenizedName ) )
                    {
                        if ( !isExcluded( name, tokenizedName ) )
                        {
                            filesIncluded.addElement( name );
                        }
                        else
                        {
                            everythingIncluded = false;
                            filesExcluded.addElement( name );
                        }
                    }
                    else
                    {
                        everythingIncluded = false;
                        filesNotIncluded.addElement( name );
                    }
                }
                else
                {
                    throw new IllegalStateException( "Should not be here" );
                }
            }
        }


        /**
         * Returns the names of the files which matched at least one of the include patterns and none of the exclude
         * patterns. The names are relative to the base directory.
         *
         * @return the names of the files which matched at least one of the include patterns and none of the exclude
         * patterns.
         */
        public String[] getIncludedFiles()
        {
            String[] files = new String[filesIncluded.size()];
            filesIncluded.copyInto( files );
            return files;
        }

        /**
         * Returns the names of the directories which matched at least one of the include patterns and none of the
         * exclude patterns. The names are relative to the base directory.
         *
         * @return the names of the directories which matched at least one of the include patterns and none of the
         * exclude patterns.
         */
        public String[] getIncludedDirectories()
        {
            String[] directories = new String[dirsIncluded.size()];
            dirsIncluded.copyInto( directories );
            return directories;
        }

        @Override
        public File getBasedir()
        {
            throw new IllegalStateException( "Should not be here" );
        }

        /**
         * <p>Checks whether the parent of this file is a symbolic link.</p>
         *
         * <p>For java versions prior to 7 It doesn't really test for symbolic links but whether the canonical and
         * absolute paths of the file are identical - this may lead to false positives on some platforms.</p>
         *
         * @param parent the parent directory of the file to test
         * @param name   the name of the file to test.
         * @return true if it's a symbolic link
         * @throws IOException .
         * @since Ant 1.5
         */
        public boolean isParentSymbolicLink( File parent, String name )
                throws IOException
        {
            if ( Java7Detector.isJava7() )
            {
                return NioFiles.isSymbolicLink( parent );
            }
            File resolvedParent = new File( parent.getCanonicalPath() );
            File toTest = new File( resolvedParent, name );
            return !toTest.getAbsolutePath().equals( toTest.getCanonicalPath() );
        }

        static String[] tokenizePathToString( @Nonnull String path, @Nonnull String separator )
        {
            List<String> ret = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer( path, separator );
            while ( st.hasMoreTokens() )
            {
                ret.add( st.nextToken() );
            }
            return ret.toArray( new String[ret.size()] );
        }
    }

    /**
     * Java7 feature detection
     *
     * @author Kristian Rosenvold
     */
    static class Java7Detector
    {

        private static final boolean IS_JAVA_7;

        static
        {
            boolean isJava7x = true;
            try
            {
                Class.forName( "java.nio.file.Files" );
            }
            catch ( Exception e )
            {
                isJava7x = false;
            }
            IS_JAVA_7 = isJava7x;
        }

        public static boolean isJava7()
        {
            return IS_JAVA_7;
        }
    }


}
