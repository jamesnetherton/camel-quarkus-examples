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
package org.apache.camel.quarkus.examples.utils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.quarkus.arc.All;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.examples.utils.client.SimpleHttpClient;
import org.apache.camel.quarkus.examples.utils.config.Configuration;
import org.apache.camel.quarkus.examples.utils.maven.GAV;
import org.apache.camel.quarkus.examples.utils.maven.MavenRunner;
import org.apache.camel.quarkus.examples.utils.transformer.post.SourcePostSyncTransformer;
import org.apache.camel.quarkus.examples.utils.transformer.pre.SourcePreSyncTransformer;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

@ApplicationScoped
@QuarkusMain
public class SyncUpstreamExamples implements QuarkusApplication {
    private static final Logger LOG = Logger.getLogger(SyncUpstreamExamples.class);
    private static final Set<String> ALLOWED_UNPRODUCTIZED_DEPENDENCIES = Set.of(
            "com.ibm.mq:com.ibm.mq.jakarta.client",
            "io.quarkus:quarkus-h2",
            "io.quarkus:quarkus-flyway",
            "io.strimzi:kafka-oauth-client",
            "org.flywaydb:flyway-mysql");

    @Inject
    Configuration configuration;

    @Inject
    SimpleHttpClient client;

    @Inject
    MavenRunner mavenRunner;

    @Inject
    @All
    List<SourcePreSyncTransformer> sourcePreTransformers;

    @Inject
    @All
    List<SourcePostSyncTransformer> sourcePostTransformers;

    public static void main(String... args) throws Exception {
        Quarkus.run(SyncUpstreamExamples.class, args);
    }

    @Override
    public int run(String... args) {
        try {
            if (configuration.isSelfUpdate()) {
                // Update this project with the desired Maven BOM GAVs etc
                selfUpdate();
            } else {
                // Configure a set of known dependencies that we always want to ignore
                configuration.getIgnoredDependencies().addAll(ALLOWED_UNPRODUCTIZED_DEPENDENCIES);

                // Download the community example projects as a GitHub source archive for the target branch
                client.downloadGitHubBranchArchive();
                // Unzip archive contents
                extractZipFile();
                // Validate example project dependencies and sync content
                syncProjects();
            }
        } catch (Exception e) {
            LOG.error("Error occurred while syncing projects", e);
            return 1;
        }

        return 0;
    }

    void extractZipFile() throws Exception {
        Path zipFile = configuration.getArchivePath();
        Path parent = zipFile.getParent();
        Path extractedDir = parent.resolve(zipFile.getFileName().toString().replace(".zip", ""));

        if (configuration.isUseCache() && Files.exists(extractedDir)) {
            return;
        } else if (!configuration.isUseCache() && Files.exists(extractedDir)) {
            FileUtils.deleteDirectory(extractedDir.toFile());
        }

        LOG.info("Extracting " + zipFile);
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String fileName = entry.getName();
                Path filePath = parent.resolve(fileName);
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    void syncProjects() throws Exception {
        Path downloadDestination = configuration.getExtractedArchivePath();
        Map<String, Set<GAV>> projectDependencies = new TreeMap<>();

        LOG.info("⚙️ Analyzing example projects. This may take some time if dependencies are not yet cached...");
        Files.walkFileTree(downloadDestination, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Perform any custom pre-processing on specific files
                sourcePreTransformers.stream()
                        .filter(sourceTransformer -> sourceTransformer.canApply(file))
                        .forEach(sourceTransformer -> sourceTransformer.transform(file));

                if (attrs.isRegularFile() && file.getFileName().toString().equals("pom.xml")) {
                    Path exampleProjectDirectory = file.getParent();
                    String exampleProjectName = exampleProjectDirectory.getFileName().toString();

                    if (!configuration.getIgnoredProjects().contains(exampleProjectName)) {
                        try {
                            LOG.info("✨ Analyzing example project " + exampleProjectName);
                            Set<GAV> dependencies = getDependencies(exampleProjectDirectory, file);

                            // Handle multi-module projects
                            Path parentPomXml = exampleProjectDirectory.getParent().resolve("pom.xml");
                            if (Files.exists(parentPomXml)) {
                                exampleProjectName = parentPomXml.getParent().getFileName() + "/" + exampleProjectName;
                            }

                            projectDependencies.put(exampleProjectName, dependencies);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        LOG.warnf("❌ Skipping project %s as it is configured as ignored", exampleProjectName);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        for (Map.Entry<String, Set<GAV>> entry : projectDependencies.entrySet()) {
            Set<GAV> unproductizedDeps = entry.getValue()
                    .stream()
                    .filter(GAV::isUnProductized)
                    .collect(Collectors.toUnmodifiableSet());
            if (unproductizedDeps.isEmpty() || configuration.isForce()) {
                String message = "all runtime dependencies are productized";
                if (configuration.isForce()) {
                    message = "the force option is true";
                }

                LOG.infof("✅ Syncing project %s as %s", entry.getKey(), message);
                entry.getValue().forEach(gav -> LOG.info("    " + gav.toString()));

                if (configuration.isAnalyzeOnly()) {
                    LOG.warnf("❌ Skipping sync of project %s as analyze-only enabled", entry.getKey());
                    continue;
                }

                Path projectToSync = downloadDestination.resolve(entry.getKey());
                Files.walk(projectToSync).forEach(source -> {
                    try {
                        Path destination = configuration.getWorkDir().resolve(downloadDestination.relativize(source));
                        if (Files.isDirectory(destination) && Files.exists(destination)) {
                            return;
                        }

                        if (!FileUtils.contentEquals(source.toFile(), destination.toFile()) || configuration.isForce()) {
                            LOG.debugf("Syncing file %s to %s", source, destination);
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            LOG.debugf("Not syncing file %s to %s. Content already matches", source, destination);
                        }

                        // Perform any custom post-processing on specific files
                        sourcePostTransformers.stream()
                                .filter(sourceTransformer -> sourceTransformer.canApply(destination))
                                .forEach(sourceTransformer -> sourceTransformer.transform(destination));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                LOG.warnf("❌ Skipping sync of project %s as not all runtime dependencies are productized", entry.getKey());
                unproductizedDeps.forEach(gav -> LOG.warn("    " + gav.toString()));
            }
        }
    }

    Set<GAV> getDependencies(Path exampleProjectDirectory, Path pomXml) throws Exception {
        Path dependencyListOutput = configuration.getTmpDir().resolve("camel-quarkus-examples-deps.txt");
        List<String> mavenArgs = List.of(
                "-f",
                pomXml.toAbsolutePath().toString(),
                "dependency:list",
                "-DincludeScope=compile",
                "-DexcludeTransitive",
                "-DoutputFile=" + dependencyListOutput.toAbsolutePath(),
                "-Dquarkus.platform.version=" + configuration.getQuarkusVersion(),
                "-Dquarkus.platform.group-id=" + configuration.getQuarkusGroupId(),
                "-Dquarkus.platform.artifact-id=" + configuration.getQuarkusArtifactId(),
                "-Dcamel-quarkus.platform.version=" + configuration.getCamelQuarkusVersion(),
                "-Dcamel-quarkus.platform.group-id=" + configuration.getCamelQuarkusGroupId(),
                "-Dcamel-quarkus.platform.artifact-id=" + configuration.getCamelQuarkusArtifactId());

        mavenRunner.run(exampleProjectDirectory, mavenArgs);

        Set<GAV> projectDependencies = new HashSet<>();
        if (Files.exists(dependencyListOutput)) {
            for (String line : Files.readAllLines(dependencyListOutput)) {
                String[] gavParts = line.trim().split(":");
                if (gavParts.length != 5) {
                    continue;
                }
                boolean isIgnored = configuration.getIgnoredDependencies().contains(gavParts[0] + ":" + gavParts[1]);
                GAV gav = new GAV(gavParts[0], gavParts[1], gavParts[3], isIgnored);
                projectDependencies.add(gav);
            }
        }

        return projectDependencies;
    }

    void selfUpdate() throws Exception {
        String workDirProp = System.getProperty("user.dir");
        Path userDir = Paths.get(workDirProp);

        if (userDir.getFileName().toString().equals("utils")) {
            Path pomXml = userDir.resolve("pom.xml");
            LOG.infof("Applying self update to %s", pomXml.toAbsolutePath());
            Stream.concat(sourcePreTransformers.stream(), sourcePostTransformers.stream()).forEach(transformer -> {
                if (transformer.canApply(pomXml)) {
                    transformer.transform(pomXml);
                }
            });
        } else {
            throw new Exception("Self update can only be run from the utils directory");
        }
    }
}
