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
package org.apache.camel.quarkus.examples.utils.profile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.quarkus.examples.utils.common.TestUtil;
import org.apache.commons.io.FileUtils;

public class MinimalConfigurationTestResource implements QuarkusTestResourceLifecycleManager {
    private Map<String, String> config;
    private Path tmpDir;

    @Override
    public void init(Map<String, String> initArgs) {
        this.config = initArgs;
    }

    @Override
    public Map<String, String> start() {
        try {
            Path community = Paths.get("src/test/resources/scenarios/minimal/community");
            Path product = Paths.get("src/test/resources/scenarios/minimal/product");

            Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve(config.get("targetDir"));
            Files.createDirectories(tmpDir);

            Path workDir = tmpDir.resolve(config.get("workDir"));
            Path archivePath = tmpDir.resolve("camel-quarkus-examples-3.8.x.zip");

            FileUtils.copyDirectory(product.toFile(), workDir.toFile());
            TestUtil.zipDirectory("camel-quarkus-examples-3.8.x", community, archivePath);

            return Map.of(
                    "quarkus.platform.version", config.get("quarkus.platform.version"),
                    "tmpDir", tmpDir.toAbsolutePath().toString(),
                    "workDir", workDir.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
    }
}
