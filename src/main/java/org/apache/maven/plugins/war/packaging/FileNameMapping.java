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
package org.apache.maven.plugins.war.packaging;

import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Evaluates file name mapping expressions such as {@code @{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@}
 * against an {@link Artifact}. Any getter of the artifact or of its artifact handler can be used as a token, plus
 * {@code dashClassifier} and {@code dashClassifier?}, which expand to {@code -<classifier>} or, for a blank classifier, to nothing.
 */
final class FileNameMapping {

    private FileNameMapping() {
        // prevent instantiation.
    }

    /**
     * Evaluates the specified expression for the given artifact.
     *
     * @param expression the expression to evaluate
     * @param artifact the artifact to use as value object for tokens
     * @return the evaluated expression
     * @throws InterpolationException in case of an error
     */
    static String evaluate(String expression, Artifact artifact) throws InterpolationException {
        RegexBasedInterpolator interpolator = new RegexBasedInterpolator("\\@\\{(", ")?([^}]+)\\}@");
        interpolator.addValueSource(new ObjectBasedValueSource(artifact));
        interpolator.addValueSource(new ObjectBasedValueSource(artifact.getArtifactHandler()));

        // Support for special expressions, like @{dashClassifier?}@, see MWAR-212
        interpolator.addValueSource(dashClassifierValueSource(artifact.getClassifier()));

        return interpolator.interpolate(expression, "__artifact");
    }

    private static PropertiesBasedValueSource dashClassifierValueSource(String classifier) {
        Properties classifierMask = new Properties();
        String trimmed = classifier == null ? "" : classifier.trim();

        if (!trimmed.isEmpty()) {
            classifierMask.setProperty("dashClassifier?", "-" + trimmed);
            classifierMask.setProperty("dashClassifier", "-" + trimmed);
        } else {
            classifierMask.setProperty("dashClassifier?", "");
            classifierMask.setProperty("dashClassifier", "");
            // Make sure that the classifier property is usable, if it is null
            classifierMask.setProperty("classifier", "");
        }
        return new PropertiesBasedValueSource(classifierMask);
    }
}
