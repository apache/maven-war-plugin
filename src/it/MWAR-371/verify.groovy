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

def customA1 = new File(basedir, 'custom/target/custom-' + projectVersion + '/x/a1.txt');
assert customA1.exists()
assert customA1.text.contains('i\'m custom')
//
def customA2 = new File(basedir, 'custom/target/custom-' + projectVersion + '/x/a2.txt');
assert customA2.exists()
assert customA2.text.contains('i\'m custom')
//
def genericA1 = new File(basedir, 'generic/target/generic-' + projectVersion + '/x/a1.txt');
assert genericA1.exists()
assert !genericA1.text.contains('i\'m custom')
//
def genericA2 = new File(basedir, 'generic/target/generic-' + projectVersion + '/x/a2.txt');
assert genericA2.exists()
assert !genericA2.text.contains('i\'m custom')

