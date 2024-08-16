/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.examples.utils.maven;

import java.util.Objects;

public class GAV {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final boolean ignored;

    public GAV(String groupId, String artifactId, String version, boolean ignored) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.ignored = ignored;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public boolean isUnProductized() {
        return !ignored && !version.contains(".redhat");
    }

    public boolean isIgnored() {
        return ignored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GAV gav = (GAV) o;
        return Objects.equals(groupId, gav.groupId) && Objects.equals(artifactId, gav.artifactId)
                && Objects.equals(version, gav.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        if (ignored) {
            return "%s:%s:%s (unproductized but allowed)".formatted(groupId, artifactId, version);
        } else {
            return "%s:%s:%s".formatted(groupId, artifactId, version);
        }
    }
}
