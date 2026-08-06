<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven WAR Plugin
The WAR Plugin is responsible for collecting all artifact dependencies, classes and resources of the web application and packaging them into a web application archive.

## Goals Overview

- [war:war](./war-mojo.html) is the default goal invoked during the `package` phase for projects with a packaging type of `war`. It builds a WAR file.
- [war:exploded](./exploded-mojo.html) is generally used to speed up testing during the developement phase by creating an exploded webapp in a specified directory.
- [war:inplace](./inplace-mojo.html) another variation of `war:explode` where the webapp is instead generated in the web application source directory, which is `src/main/webapp` by default.
## Usage

General instructions on how to use the WAR Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below. To share common resources across multiple web applications, see the documentation about using [overlays](./overlays.html).

If you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you think the plugin is missing a feature or has a defect, file a feature request or bug report in the [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

To provide you with better understanding on some usages of the Maven WAR Plugin, you can look into the following examples:

- [Adding and Filtering External Web Resources](./examples/adding-filtering-webresources.html)
- [WAR Manifest Customization](./examples/war-manifest-guide.html)
- [Rapid Testing the Jetty Plugin](./examples/rapid-testing-jetty6-plugin.html)
- [Creating Skinny WARs](https://maven.apache.org/plugins/maven-ear-plugin/examples/skinny-wars.html)
- [Including and Excluding Files From the WAR](./examples/including-excluding-files-from-war.html)
- [Using File Name Mapping](./examples/file-name-mapping.html)
## Related links

- [Exclusion of Maven Descriptors](/guides/mini/guide-archive-configuration.html)
