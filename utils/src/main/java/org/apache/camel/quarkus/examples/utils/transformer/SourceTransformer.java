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
package org.apache.camel.quarkus.examples.utils.transformer;

import java.nio.file.Path;

/**
 * Abstraction for applying customizations to example project source files.
 */
public interface SourceTransformer {
    /**
     * Whether the transformer can run.
     *
     * @param  source The source file to be transformed
     * @return        {@code true} if the source file can be transformed. {@code false} if transformations should not be
     *                applied.
     */
    boolean canApply(Path source);

    /**
     * Applies transformations to the given file.
     *
     * @param source The path to the source file to transform
     */
    void transform(Path source);
}
