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
package org.apache.camel.quarkus.examples.utils.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class Configuration {
    private static final String GITHUB_BASE_URL = "https://github.com/apache/camel-quarkus-examples/archive/refs/heads/%s.zip";
    private static final Path WORK_DIR;
    private final Set<String> internalBannedDependencies = new HashSet<>();

    @ConfigProperty(name = "quarkus.platform.group-id", defaultValue = "com.redhat.quarkus.platform")
    String quarkusGroupId;

    @ConfigProperty(name = "quarkus.platform.artifact-id", defaultValue = "quarkus-bom")
    String quarkusArtifactId;

    @ConfigProperty(name = "quarkus.platform.version")
    String quarkusVersion;

    @ConfigProperty(name = "camel-quarkus.platform.group-id", defaultValue = "com.redhat.quarkus.platform")
    Optional<String> camelQuarkusGroupId;

    @ConfigProperty(name = "camel-quarkus.platform.artifact-id", defaultValue = "quarkus-camel-bom")
    String camelQuarkusArtifactId;

    @ConfigProperty(name = "camel-quarkus.platform.version")
    Optional<String> camelQuarkusVersion;

    @ConfigProperty(name = "force", defaultValue = "false")
    boolean force;

    @ConfigProperty(name = "debug", defaultValue = "false")
    boolean debug;

    @ConfigProperty(name = "analyzeOnly", defaultValue = "false")
    boolean analyzeOnly;

    @ConfigProperty(name = "useCache", defaultValue = "true")
    boolean useCache;

    @ConfigProperty(name = "selfUpdate", defaultValue = "false")
    boolean selfUpdate;

    @ConfigProperty(name = "workDir")
    Optional<Path> workDir;

    @ConfigProperty(name = "tmpDir", defaultValue = "java.io.tmpdir")
    Path tmpDir;

    @ConfigProperty(name = "settingsXml")
    Optional<Path> settingsXml;

    @ConfigProperty(name = "ignoredProjects")
    Optional<Set<String>> ignoredProjects;

    @ConfigProperty(name = "ignoredDependencies")
    Optional<Set<String>> ignoredDependencies;

    static {
        String workDirProp = System.getProperty("user.dir");
        Path userDir = Paths.get(workDirProp);

        if (userDir.getFileName().toString().startsWith("camel-quarkus-examples")) {
            WORK_DIR = userDir;
        } else if (userDir.getFileName().toString().equals("utils")) {
            WORK_DIR = userDir.resolve("../");
        } else {
            WORK_DIR = null;
        }
    }

    public String getQuarkusVersion() {
        return quarkusVersion;
    }

    public String getQuarkusGroupId() {
        return quarkusGroupId;
    }

    public String getQuarkusArtifactId() {
        return quarkusArtifactId;
    }

    public String getCamelQuarkusVersion() {
        return camelQuarkusVersion.orElse(quarkusVersion);
    }

    public String getCamelQuarkusGroupId() {
        return camelQuarkusGroupId.orElse(quarkusGroupId);
    }

    public String getCamelQuarkusArtifactId() {
        return camelQuarkusArtifactId;
    }

    public String getCamelQuarkusExamplesVersion() {
        return getCamelQuarkusVersion().substring(0, 3) + ".0-redhat-00001";
    }

    public String getGitHubDownloadBaseUrl() {
        return GITHUB_BASE_URL;
    }

    public Path getArchivePath() {
        return getTmpDir().resolve("camel-quarkus-examples-%s.zip".formatted(getUpstreamBranchName()));
    }

    public URL getGitHubBranchArchiveURL() {
        String url = getGitHubDownloadBaseUrl().formatted(getUpstreamBranchName());
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUpstreamBranchName() {
        String[] versionParts = getCamelQuarkusVersion().split("\\.");
        return "%s.%s.x".formatted(versionParts[0], versionParts[1]);
    }

    public Path getExtractedArchivePath() {
        return getTmpDir().resolve("camel-quarkus-examples-%s".formatted(getUpstreamBranchName()));
    }

    public boolean isForce() {
        return force;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isAnalyzeOnly() {
        return analyzeOnly;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public boolean isSelfUpdate() {
        return selfUpdate;
    }

    public Path getTmpDir() {
        return tmpDir;
    }

    public Path getWorkDir() {
        return workDir.orElse(WORK_DIR);
    }

    public Path getSettingsXml() {
        return settingsXml.orElse(null);
    }

    public Set<String> getIgnoredProjects() {
        return ignoredProjects.orElse(Collections.emptySet());
    }

    public Set<String> getIgnoredDependencies() {
        return ignoredDependencies.orElse(internalBannedDependencies);
    }
}
