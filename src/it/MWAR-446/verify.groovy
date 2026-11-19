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

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

def warFile = new File(basedir, 'target/MWAR-446-0.0.1-SNAPSHOT.war')
assert warFile.exists() : "WAR file should exist"

def zipFile = new ZipFile(warFile)
try {
    def foundStoredJar = false
    def foundDeflatedEntry = false

    zipFile.entries().each { ZipEntry entry ->
        if (entry.name.startsWith('WEB-INF/lib/') && entry.name.endsWith('.jar')) {
            assert entry.method == ZipEntry.STORED :
                "JAR entry ${entry.name} should be STORED (method=0) but was method=${entry.method}"
            foundStoredJar = true
        }
        if (entry.name == 'index.html') {
            assert entry.method == ZipEntry.DEFLATED :
                "index.html should be DEFLATED (method=8) but was method=${entry.method}"
            foundDeflatedEntry = true
        }
    }

    assert foundStoredJar : "Should have found at least one JAR in WEB-INF/lib/"
    assert foundDeflatedEntry : "Should have found index.html"
} finally {
    zipFile.close()
}
