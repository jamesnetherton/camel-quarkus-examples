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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.apache.camel.quarkus.examples.utils.common.FileDiffType;
import org.apache.camel.quarkus.examples.utils.common.TestUtil;
import org.apache.camel.quarkus.examples.utils.profile.MinimalConfigurationTestResource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusMainTest
@QuarkusTestResource(value = MinimalConfigurationTestResource.class, initArgs = {
        @ResourceArg(name = "quarkus.platform.version", value = "3.8.5.SP1-redhat-00001"),
        @ResourceArg(name = "workDir", value = "camel-quarkus-examples-productized"),
        @ResourceArg(name = "targetDir", value = "simple-config-test"),
})
class MinimalConfigurationTest {
    @AfterAll
    public static void afterAll() {
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"));
            FileUtils.deleteDirectory(path.resolve("simple-config-test").toFile());
        } catch (IOException e) {
            // Ignored
        }
    }

    @Launch
    @Test
    void syncProjectsWithMinimalConfiguration() throws Exception {
        Path community = Paths.get("src/test/resources/scenarios/minimal/community");

        Path targetDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve("simple-config-test")
                .resolve("camel-quarkus-examples-productized");

        Map<FileDiffType, List<String>> comparison = TestUtil.compareCommunityAndProductExamples(community, targetDir);
        List<String> missingFiles = comparison.get(FileDiffType.MISSING_FILE_OR_DIRECTORY);

        assertEquals(3, missingFiles.size());
        // Kubernetes manifests should not be synchronized
        assertTrue(missingFiles.get(0).endsWith("kubernetes.yml"));
        // The 'bar' project directory should not be synchronized as it contains non-productized dependencies
        assertTrue(missingFiles.get(1).endsWith("bar/pom.xml"));
        assertTrue(missingFiles.get(2).endsWith("Bar.java"));

        List<String> fileContentDiffs = comparison.get(FileDiffType.CONTENT_DIFFERS);

        assertEquals(2, fileContentDiffs.size());
        assertTrue(fileContentDiffs.get(0).endsWith("pom.xml"));
        assertTrue(fileContentDiffs.get(1).endsWith("README.adoc"));

        String pomXml = Files.readString(targetDir.resolve("foo/pom.xml"));
        // Productized versions should be applied
        assertTrue(pomXml.contains("<version>3.8.0-redhat-00001</version>"));
        assertTrue(pomXml.contains("<quarkus.platform.version>3.8.5.SP1-redhat-00001</quarkus.platform.version>"));
        // MRRC configuration should be applied
        assertTrue(pomXml.contains("redhat-ga-repository"));
        assertTrue(pomXml.contains("redhat-earlyaccess-repository"));
        // Only OpenShift profiles should remain
        assertTrue(pomXml.contains("<id>openshift</id>"));
        assertFalse(pomXml.contains("<id>kubernetes</id>"));
        // Explicit Quarkus Artemis versions should be removed
        assertFalse(pomXml.contains("<version>${quarkiverse-artemis.version}</version>"));

        String readme = Files.readString(targetDir.resolve("foo/README.adoc"));
        // Only OpenShift cloud deployment instructions should remain
        assertTrue(readme.contains("Deploying to OpenShift"));
        assertFalse(readme.contains("Deploying to Kubernetes"));
    }
}
