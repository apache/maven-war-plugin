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

import java.util.jar.*;

File explodedDir = new File( basedir, "target/maven-it-it0016-1.0" );
System.out.println( "Checking for existence of exploded directory " + explodedDir );
assert explodedDir.isDirectory()

String[] expectedPaths = [
        "index.html",
        "WEB-INF/classes/org/apache/maven/it0016/Person.class",
        "WEB-INF/lib/commons-logging-1.0.3.jar",
];
for ( String path : expectedPaths )
{
    File file = new File( explodedDir, path );
    System.out.println( "Checking for existence of " + file );
    assert file.exists()
}

String[] unexpectedPaths = [
        "WEB-INF/lib/servlet-api-2.4.jar",
];
for ( String path : unexpectedPaths )
{
    File file = new File( explodedDir, path );
    System.out.println( "Checking for absence of " + file );
    assert !file.exists()
}

File warFile = new File( basedir, "target/maven-it-it0016-1.0.war" );
System.out.println( "Checking for existence of " + warFile );
assert warFile.isFile()

JarFile war = new JarFile( warFile );

String[] includedEntries = [
    "WEB-INF/classes/org/apache/maven/it0016/Person.class",
    "WEB-INF/lib/commons-logging-1.0.3.jar",
];
for ( String included : includedEntries )
{
    System.out.println( "Checking for existence of " + included );
    assert war.getEntry( included ) != null
}

war.close();

return true;
