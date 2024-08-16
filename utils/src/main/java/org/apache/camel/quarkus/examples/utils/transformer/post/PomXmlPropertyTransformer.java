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
package org.apache.camel.quarkus.examples.utils.transformer.post;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.examples.utils.config.Configuration;

/**
 * Post sync processor to update pom.xml with productized Maven GAVs.
 */
@Singleton
public class PomXmlPropertyTransformer implements SourcePostSyncTransformer {
    @Inject
    Configuration configuration;

    @Override
    public void transform(Path source) {
        try {
            String content = Files.readString(source);
            String updatedContent = updateMavenProperty(content, "version", configuration.getCamelQuarkusExamplesVersion());
            updatedContent = updateMavenProperty(updatedContent, "quarkus.platform.group-id",
                    configuration.getQuarkusGroupId());
            updatedContent = updateMavenProperty(updatedContent, "quarkus.platform.artifact-id",
                    configuration.getQuarkusArtifactId());
            updatedContent = updateMavenProperty(updatedContent, "quarkus.platform.version", configuration.getQuarkusVersion());

            if (!configuration.getCamelQuarkusGroupId().equals(configuration.getQuarkusGroupId())) {
                updatedContent = updateMavenProperty(updatedContent, "camel-quarkus.platform.group-id",
                        configuration.getCamelQuarkusGroupId());
            }

            updatedContent = updateMavenProperty(updatedContent, "camel-quarkus.platform.artifact-id",
                    configuration.getCamelQuarkusArtifactId());

            if (!configuration.getCamelQuarkusVersion().equals(configuration.getQuarkusVersion())) {
                updatedContent = updateMavenProperty(updatedContent, "camel-quarkus.platform.version",
                        configuration.getCamelQuarkusVersion());
            }
            Files.writeString(source, updatedContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canApply(Path source) {
        return source.getFileName().toString().equals("pom.xml");
    }

    private String updateMavenProperty(String content, String name, String value) {
        if (configuration.isSelfUpdate()) {
            name = name.replaceFirst("^quarkus\\.", "");
        }
        return content.replaceFirst("(<" + name + ">)(.*?)(</" + name + ">)", "$1" + value + "$3");
    }
}
