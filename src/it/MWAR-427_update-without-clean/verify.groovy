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

// outdated-default: MWAR-427 and MWAR-433, only WEB-INF/lib/ is checked for outdated files by default
def defaultWar = new java.util.jar.JarFile( new File( basedir, "outdated-default/target/mwar427-1.0-SNAPSHOT.war" ), false )
assert defaultWar.getEntry( 'WEB-INF/lib/plexus-utils-3.0.23.jar' ) != null : "updated dependency missing"
assert defaultWar.getEntry( 'WEB-INF/lib/mwar427-1.0-SNAPSHOT.jar' ) != null : "archived classes missing"
assert defaultWar.getEntry( 'index.html' ) != null : "renamed page missing"
assert defaultWar.getEntry( "WEB-INF/lib/plexus-utils-" + plexusUtilVersion + ".jar" ) == null : "outdated dependency not removed"
// other resources may be generated outside the plugin, so root.html is not removed
assert defaultWar.getEntry( 'root.html' ) != null : "root.html must stay with the default outdatedCheckPath"

// outdated-root: MWAR-441, outdatedCheckPath=/ removes every outdated file
def rootWar = new java.util.jar.JarFile( new File( basedir, "outdated-root/target/mwar441-1.0-SNAPSHOT.war" ), false )
assert rootWar.getEntry( 'WEB-INF/lib/plexus-utils-3.0.23.jar' ) != null : "updated dependency missing"
assert rootWar.getEntry( 'WEB-INF/lib/mwar441-1.0-SNAPSHOT.jar' ) != null : "archived classes missing"
assert rootWar.getEntry( 'index.html' ) != null : "renamed page missing"
assert rootWar.getEntry( "WEB-INF/lib/plexus-utils-" + plexusUtilVersion + ".jar" ) == null : "outdated dependency not removed"
assert rootWar.getEntry( 'root.html' ) == null : "outdated root.html not removed with outdatedCheckPath=/"
