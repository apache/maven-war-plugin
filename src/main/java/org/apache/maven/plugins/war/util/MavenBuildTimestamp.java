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
package org.apache.maven.plugins.war.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Formats the build timestamp the same way Maven core does when interpolating {@code ${maven.build.timestamp}}
 * in the POM. Duplicated from {@code maven-model-builder} because that computation isn't reachable from a plugin
 * (same reasoning as MRESOURCES-99 in {@code maven-resources-plugin}): the value substituted into the POM itself
 * isn't exposed through the build context a plugin can obtain from Maven core.
 *
 * @since 3.5.2
 */
public class MavenBuildTimestamp {

    /**
     * ISO 8601-compliant timestamp for machine readability.
     */
    public static final String DEFAULT_BUILD_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * The property used to override {@link #DEFAULT_BUILD_TIMESTAMP_FORMAT}.
     */
    public static final String BUILD_TIMESTAMP_FORMAT_PROPERTY = "maven.build.timestamp.format";

    /**
     * The default time zone, {@code Etc/UTC}.
     */
    public static final TimeZone DEFAULT_BUILD_TIME_ZONE = TimeZone.getTimeZone("Etc/UTC");

    private final String formattedTimestamp;

    /**
     * Creates an instance formatting the current time with the default format.
     */
    public MavenBuildTimestamp() {
        this(new Date());
    }

    /**
     * Creates an instance formatting the given time with the default format.
     *
     * @param time the time to format
     */
    public MavenBuildTimestamp(final Date time) {
        this(time, DEFAULT_BUILD_TIMESTAMP_FORMAT);
    }

    /**
     * Creates an instance formatting the given time, taking the format from {@link #BUILD_TIMESTAMP_FORMAT_PROPERTY}
     * when present in {@code properties}, or the default format otherwise.
     *
     * @param time the time to format
     * @param properties properties that may carry {@link #BUILD_TIMESTAMP_FORMAT_PROPERTY}, may be {@code null}
     */
    public MavenBuildTimestamp(final Date time, final Properties properties) {
        this(time, properties != null ? properties.getProperty(BUILD_TIMESTAMP_FORMAT_PROPERTY) : null);
    }

    /**
     * Creates an instance formatting the given time with the given format.
     *
     * @param time the time to format
     * @param timestampFormat the {@link SimpleDateFormat} pattern to use, or {@code null} for the default
     */
    public MavenBuildTimestamp(final Date time, final String timestampFormat) {
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat(timestampFormat == null ? DEFAULT_BUILD_TIMESTAMP_FORMAT : timestampFormat);
        dateFormat.setCalendar(new GregorianCalendar());
        dateFormat.setTimeZone(DEFAULT_BUILD_TIME_ZONE);

        formattedTimestamp = dateFormat.format(time == null ? new Date() : time);
    }

    /**
     * @return the formatted timestamp
     */
    public String formattedTimestamp() {
        return formattedTimestamp;
    }
}
