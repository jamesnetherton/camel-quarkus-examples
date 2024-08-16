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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.examples.utils.config.Configuration;
import org.jboss.logging.Logger;

@Singleton
public class MavenRunner {
    private static final Logger LOG = Logger.getLogger(MavenRunner.class);

    @Inject
    Configuration configuration;

    public void run(Path processWorkDir, List<String> args) throws Exception {
        List<String> baseArgs = List.of("/bin/bash", "-C", findMvnw(processWorkDir).toAbsolutePath().toString(), "-Pprod");
        List<String> mavenArgs = new ArrayList<>(baseArgs);
        mavenArgs.addAll(args);

        if (configuration.getSettingsXml() != null) {
            mavenArgs.add("-s");
            mavenArgs.add(configuration.getSettingsXml().toAbsolutePath().toString());
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(processWorkDir.toFile());
        builder.command(mavenArgs.toArray(new String[0]));
        if (configuration.isDebug()) {
            builder.inheritIO();
        }

        Process process = builder.start();
        process.waitFor();
        if (process.exitValue() != 0) {
            LOG.errorf("Failed running Maven on project %s", processWorkDir.getFileName());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    LOG.errorf(line);
                    line = reader.readLine();
                }
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    LOG.errorf(line);
                    line = reader.readLine();
                }
            }
            throw new Exception("Maven command failed with exit code " + process.exitValue());
        }
    }

    Path findMvnw(Path startingDirectory) {
        Path resolve = startingDirectory.resolve("mvnw");
        if (Files.exists(resolve)) {
            return resolve;
        }
        return findMvnw(startingDirectory.getParent());
    }
}
