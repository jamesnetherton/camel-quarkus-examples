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
package org.apache.camel.quarkus.examples.utils.transformer.pre;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Singleton;

/**
 * Pre sync processor to update README.adoc and remove the kubernetes section.
 */
@Singleton
public class KubernetesReadmeTransformer implements SourcePreSyncTransformer {
    @Override
    public void transform(Path source) {
        try {
            String content = Files.readString(source);
            String regex = "(?s)Deploying to Kubernetes.*?(?=Deploying to OpenShift)";
            String updatedContent = content.replaceFirst(regex, "");
            if (!content.equals(updatedContent)) {
                Files.writeString(source, updatedContent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canApply(Path source) {
        return source.getFileName().toString().equals("README.adoc");
    }
}
