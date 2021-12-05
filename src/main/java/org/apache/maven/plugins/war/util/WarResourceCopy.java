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

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Skip Copying file when build war directly from source paths. Does not register filtered files that copy with
 * copyWithMavenFilter,
 */
public class WarResourceCopy
{

    private boolean skipCopy;

    private Map<String, File> sourceTargetMappings;

    public WarResourceCopy( boolean skipCopy )
    {
        this.skipCopy = skipCopy;
        this.sourceTargetMappings = new HashMap<>();
    }

    public void addFileCopy( String targetPath, File sourceFile )
    {
        if ( skipCopy )
        {
            this.sourceTargetMappings.put( targetPath, sourceFile );
        }
    }

    public Map<String, File> getSourceTargetMappings()
    {
        return this.sourceTargetMappings;
    }

    public Map<String, File> getFilesWithPrefix( String prefix, boolean removePrefix )
    {
        Map<String, File> files = new HashMap<>();
        Set<String> keys = sourceTargetMappings.keySet();
        for ( String key : keys )
        {
            if ( key.startsWith( prefix ) )
            {
                files.put( removePrefix ? key.substring( prefix.length() ) : key, sourceTargetMappings.get( key ) );
            }
        }

        return files;
    }

    public void copy( File source, File targetFile ) throws IOException
    {
        if ( !skipCopy )
        {
            FileUtils.copyFile( source.getCanonicalFile(), targetFile );
            // preserve timestamp
            targetFile.setLastModified( source.lastModified() );
        }
    }
}
