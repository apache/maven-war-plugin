
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

def aao = "\u00e5\u00e4\u00f6" // a-ring, a-umlaut and o-umlaut

def check = { String module, String finalName ->
    def webInf = new File( basedir, "${module}/target/${finalName}/WEB-INF" )
    assert webInf.isDirectory() : "${webInf} is missing or is not a directory"

    def jettyEnv = new File( webInf, "jetty-env.xml" )
    assert jettyEnv.isFile() : "${jettyEnv} is missing or is a directory"
    def jettyEnvContent = jettyEnv.getText( "UTF-8" )
    assert jettyEnvContent.contains( "Characters that should be encoded in UTF-8: ${aao}" ) : "non-ascii characters changed encoding during filtering"
    assert jettyEnvContent.contains( '<Set name="URL">jdbc:oracle:thin:@localhost:1521:orcl</Set>' ) : "jdbc.url not filtered correctly"

    def myProperties = new File( webInf, "classes/my.properties" )
    assert myProperties.isFile() : "${myProperties} is missing or is a directory"
    def properties = new Properties()
    myProperties.withInputStream { properties.load( it ) } // reads ISO-8859-1
    assert properties.getProperty( "my.property" ) == "Characters that should be encoded in ISO-8859-1: ${aao}" : "non-ascii characters have wrong encoding after filtering"

    return jettyEnvContent
}

// default delimiters, the escaped expression stays as it is
def defaultContent = check( "web", "example-web" )
assert defaultContent.contains( "Author id: 1" ) : "project.developers[0].id not filtered correctly"
assert defaultContent.contains( '<Set name="password">${jdbc.password}</Set>' ) : "jdbc.password has been filtered"

// custom @@*@@ delimiters, the escaped expression stays as it is
def delimiterContent = check( "web-delimiter", "example-web-delimiter" )
assert delimiterContent.contains( '<Set name="password">@@jdbc.password@@</Set>' ) : "jdbc.password has been filtered"
