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
package org.apache.camel.quarkus.examples.utils.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TestUtil {
    private TestUtil() {
        // Utility class
    }

    public static Map<FileDiffType, List<String>> compareCommunityAndProductExamples(Path community, Path productModified)
            throws IOException {
        assertTrue(Files.exists(productModified));
        assertTrue(Files.isDirectory(productModified));

        assertTrue(Files.exists(community));
        assertTrue(Files.isDirectory(community));

        Map<FileDiffType, List<String>> diffs = new HashMap<>();
        diffs.put(FileDiffType.MISSING_FILE_OR_DIRECTORY, new ArrayList<>());
        diffs.put(FileDiffType.CONTENT_DIFFERS, new ArrayList<>());

        Files.walkFileTree(community, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);

                Path relativize = community.relativize(file);
                Path fileInOther = productModified.resolve(relativize);

                if (!Files.exists(fileInOther)) {
                    diffs.get(FileDiffType.MISSING_FILE_OR_DIRECTORY).add(fileInOther.toAbsolutePath().toString());
                } else if (!FileUtils.contentEquals(file.toFile(), fileInOther.toFile())) {
                    diffs.get(FileDiffType.CONTENT_DIFFERS).add(file.toAbsolutePath().toString());
                }

                return result;
            }
        });

        return diffs;
    }

    public static void zipDirectory(String rootDirName, Path source, Path target) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target.toFile()));
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                zos.putNextEntry(new ZipEntry(rootDirName + "/" + source.relativize(file)));
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });

        Path mvnw = findMvnw(source.toAbsolutePath());
        zos.putNextEntry(new ZipEntry(rootDirName + "/mvnw"));
        Files.copy(mvnw, zos);
        zos.closeEntry();

        Path dotMvnw = mvnw.getParent().resolve(".mvn");

        Files.walkFileTree(dotMvnw, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                zos.putNextEntry(new ZipEntry(rootDirName + "/.mvn/" + dotMvnw.relativize(file)));
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });

        zos.close();
    }

    private static Path findMvnw(Path startingDirectory) {
        Path mvnw = startingDirectory.resolve("mvnw");
        if (Files.exists(mvnw)) {
            return mvnw;
        }
        return findMvnw(startingDirectory.getParent());
    }
}
