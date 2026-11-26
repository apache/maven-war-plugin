---
title: Using File Name Mapping
author: 
  - Stephane Nicoll
  - Dennis Lundberg
date: 2010-05-24
---

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

# Using File Name Mapping

It might be necessary to customize the file name of libraries and TLDs. By default, those resources are stored using the following pattern:

```text
@{artifactId}@-@{baseVersion}@@{dashClassifier?}@.@{extension}@
```

For example, `commons-lang3-3.17.0.jar`, `commons-lang3-3.17.0-tests.jar` for an artifact with the `tests` classifier, or `my-lib-1.0-SNAPSHOT.jar` for a `SNAPSHOT` dependency, whatever timestamp it was resolved with.

The `outputFileNameMapping` parameter allows you to give a custom pattern. Each token defined in the pattern will be replaced with a value from the current artifact. The following tokens are supported:

- `@{groupId}@` - the artifact's group ID
- `@{artifactId}@` - the artifact's ID
- `@{version}@` - the artifact version; for `SNAPSHOT` artifacts this can contain a timestamp instead of `SNAPSHOT`
- `@{baseVersion}@` - the base artifact version; for `SNAPSHOT` artifacts this always ends with `SNAPSHOT`
- `@{classifier}@` - the artifact's classifier, or nothing if it has none
- `@{extension}@` - the artifact's file extension
- `@{dashClassifier}@` - the classifier preceded by a dash, or nothing if the artifact has no classifier or a blank one
- `@{dashClassifier?}@` - same as `@{dashClassifier}@`; the spelling supported since 2.1

On this Maven 3 line, other properties of Artifact and ArtifactHandler (for example `type` or `scope`) also resolve as tokens. The Maven 4 API has no such properties, so a pattern that should work on both lines uses only the tokens listed above.

If two artifacts end up with the same file name, both are stored with their group ID as a prefix, for example `org.sample-my-lib-1.0.jar`.

For instance, to store the libraries and TLDs without version numbers or classifiers, use the following pattern:

```text
@{artifactId}@.@{extension}@
```

To store the libraries and TLDs without version numbers but with classifiers (if they exist), use the following pattern:

```text
@{artifactId}@@{dashClassifier?}@.@{extension}@
```
