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

boolean checkFile( String fileName, String artifact, String module )
{
    def customA1 = new File( basedir, "${artifact}/target/${artifact}-1.0-SNAPSHOT/x/${fileName}" )
    if ( ! customA1.exists() )
    {
        System.err.println( "${artifact}/target/${artifact}-1.0-SNAPSHOT/x/${fileName} does not exist." )
        return false
    }
    if ( ! customA1.text.contains( module ) )
    {
        System.err.println( "${artifact}/target/${artifact}-1.0-SNAPSHOT/x/${fileName} is not ${module}." )
        return false
    }
    return true
}

boolean checkFile( String fileName, String module )
{
    return checkFile( fileName, module, module )
}

try {
    if ( ! checkFile( "a1.txt", "custom" ) )
    {
        return false
    }
    if ( ! checkFile( "a2.txt", "custom" ) )
    {
        return false
    }
    if ( ! checkFile( "a3.txt", "custom", "generic" ) )
    {
        return false
    }
    if ( ! checkFile( "a1.txt", "generic" ) )
    {
        return false
    }
    if ( ! checkFile( "a2.txt", "generic" ) )
    {
        return false
    }
    if ( ! checkFile( "a3.txt", "generic" ) )
    {
        return false
    }
}
catch ( Throwable e )
{
    e.printStackTrace()
    return false
}

return true

