
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

import org.codehaus.plexus.util.*;

boolean filesAreIdentical( File expected, File current )
    throws IOException
{
    if ( expected.length() != current.length() )
    {
        return false;
    }
    FileInputStream expectedIn = new FileInputStream( expected );
    FileInputStream currentIn = new FileInputStream( current );
    try
    {
        byte[] expectedBuffer = IOUtil.toByteArray( expectedIn );

        byte[] currentBuffer = IOUtil.toByteArray( currentIn );
        if ( expectedBuffer.length != currentBuffer.length )
        {
            return false;
        }
        for ( int i = 0,size = expectedBuffer.length; i<size; i++ )
        {
            if ( expectedBuffer[i] != currentBuffer[i] )
            {
                return false;
            }
        }
    }
    finally
    {
        expectedIn.close();
        currentIn.close();
    }
    return true;
}

File originalImg = new File( basedir, "war-common/src/main/images/duke-beer.jpg" );

File targetImg = new File( basedir, "war-common/target/common-overlay-1.0-SNAPSHOT/duke-beer.jpg" );

boolean identical = filesAreIdentical( originalImg, targetImg );

assert identical : "filtered images are not identical in war-common."

File target = new File( basedir, "war-filter-overlay/target" );
assert target.exists() && target.isDirectory() : "target file is missing or not a directory."

File webappDirectory = new File( target, "custom-manifest-1.0-SNAPSHOT" );
assert webappDirectory.exists() && webappDirectory.isDirectory() : "webappDirectory is missing or not a directory."

File param = new File( webappDirectory, "WEB-INF/classes/filter.properties" );
assert param.exists() && !param.isDirectory() : "filter.properties file is missing or a directory."


String paramContent = param.getText( 'UTF-8' );

int indexOf = paramContent.indexOf( "debug.test = hello world !" );
assert indexOf >= 0 : "filter.properties does not contains debug.test = hello world !"

targetImg = new File( basedir, "war-filter-overlay/target/custom-manifest-1.0-SNAPSHOT/duke-beer.jpg" );

identical = filesAreIdentical( originalImg, targetImg );

assert identical : "filtered images are not identical with overlay."
