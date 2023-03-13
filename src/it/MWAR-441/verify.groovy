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

def warFile = new java.util.jar.JarFile( new File(basedir,"target/mwar427-1.0-SNAPSHOT.war"), false)
assert warFile.getEntry('WEB-INF/lib/plexus-utils-3.0.23.jar') != null
assert warFile.getEntry('WEB-INF/lib/mwar427-1.0-SNAPSHOT.jar') != null
assert warFile.getEntry('index.html') != null

assert warFile.getEntry('WEB-INF/lib/plexus-utils-3.0.24.jar') == null
assert warFile.getEntry('root.html') == null // after MWAR-441, this path is also removed as outdated (with the '/' config in the pom).
