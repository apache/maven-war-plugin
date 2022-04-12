
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

import java.io.*;

import org.codehaus.plexus.util.*;

boolean result = true;

try
{
    File target = new File( basedir, "target/mwar450/WEB-INF/classes" );
    if ( !target.exists() || !target.isDirectory() )
    {
        System.err.println( "target/mwar450/WEB-INF/classes is missing or is not a directory." );
        return false;
    }

    // Load and check log4j.xml
    File log4jxml = new File( target, "log4j.xml" );
    if ( !log4jxml.exists() || log4jxml.isDirectory() )
    {
        System.err.println( "log4j.xml is missing or is a directory." );
        return false;
    }
    FileInputStream fis = new FileInputStream ( log4jxml );
    String paramContent = IOUtil.toString ( fis, "UTF-8" );
    System.out.println( "content='" + paramContent + "'" );
    int indexOf = paramContent.indexOf( "This file is encoded in UTF-8 and should remain so after filtering - åäö" );
    if ( indexOf < 0 )
    {
      System.err.println( "Non-ascii characters changed encoding during filtering" );
      return false;
    }

    // Load and check my.properties
    File myProperties = new File( target, "my.properties" );
    if ( !myProperties.exists() || myProperties.isDirectory() )
    {
        System.err.println( "my.properties is missing or is a directory." );
        return false;
    }
    Properties properties = new Properties();
    FileInputStream fis = new FileInputStream( myProperties );
    properties.load( fis );
    fis.close();
    String property = properties.get( "my.property" );
    System.out.println( "my.property='" + property + "'" );
    if ( !"Characters that should be encoded in ISO-8859-1: åäö".equals( property ) )
    {
        System.err.println( "Non-ascii characters has wrong encoding after filtering" );
        return false;
    }

    File myfile = new File( target, "myfile.txt" );
    if ( !myfile.exists() || myfile.isDirectory() )
    {
        System.err.println( "myfile.txt is missing or is a directory." );
        return false;
    }
    FileInputStream fis = new FileInputStream ( myfile );
    String paramContent = IOUtil.toString ( fis, "UTF-8" );
    System.out.println( "content='" + paramContent + "'" );
    int indexOf = paramContent.indexOf( "Characters that should be encoded in UTF-8: åäö" );
    if ( indexOf < 0 )
    {
      System.err.println( "Non-ascii characters changed encoding during filtering" );
      return false;
    }
}
catch( IOException e )
{
    e.printStackTrace();
    result = false;
}

return result;
