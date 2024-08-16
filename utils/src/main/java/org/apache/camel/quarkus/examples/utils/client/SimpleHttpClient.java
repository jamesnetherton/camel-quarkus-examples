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
package org.apache.camel.quarkus.examples.utils.client;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.examples.utils.config.Configuration;
import org.jboss.logging.Logger;

@Singleton
public class SimpleHttpClient {
    private static final Logger LOG = Logger.getLogger(SimpleHttpClient.class);

    @Inject
    Configuration configuration;

    public void downloadGitHubBranchArchive() {
        URL downloadUrl = configuration.getGitHubBranchArchiveURL();
        Path downloadDestination = configuration.getArchivePath();
        if (configuration.isUseCache() && Files.exists(downloadDestination)) {
            LOG.infof("Using cached download of %s", downloadUrl);
            return;
        }

        LOG.infof("Downloading %s to %s", downloadUrl, downloadDestination);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed to download upstream branch sources from: " + downloadUrl);
            }

            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream fileOutputStream = new FileOutputStream(downloadDestination.toFile())) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
