/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
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
package com.indoqa.boot.jsapp;

import static java.nio.file.Files.*;
import static java.util.Collections.emptySet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.spring.ClassPathScanner;

public final class WebpackAssetsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebpackAssetsUtils.class);
    private static final String ASSETS_PATH = "assets/";
    private static final ClassPathScanner SCANNER = new ClassPathScanner();

    private WebpackAssetsUtils() {
        // hide utility class constructor
    }

    public static Assets findWebpackAssetsInClasspath(String folder) {
        try {
            return new WebpackAssets(SCANNER.findFiles(createAssetsClasspathFolder(folder)));
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while searching for webpack assets.", e);
        }
    }

    public static Assets findWebpackAssetsInFilesystem(String folder) {
        try {
            return new WebpackAssets(findLocalFiles(createAssetsLocalFolder(folder)));
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while searching for webpack assets.", e);
        }
    }

    private static String createAssetsClasspathFolder(String folder) {
        StringBuilder path = new StringBuilder();
        if (!folder.startsWith("/")) {
            path.append("/");
        }
        path.append(folder);

        if (!folder.endsWith("/")) {
            path.append("/");
        }
        path.append(ASSETS_PATH);
        path.append("*");

        return path.toString();
    }

    private static String createAssetsLocalFolder(String folder) {
        StringBuilder path = new StringBuilder();

        path.append(folder);

        if (!folder.endsWith("/")) {
            path.append("/");
        }
        path.append(ASSETS_PATH);

        return path.toString();
    }

    private static String extractFilename(String path) {
        int lastSeparator = path.lastIndexOf("/assets/");
        int pos = Math.min(lastSeparator, path.length());
        return path.substring(pos);
    }

    private static Set<URL> findLocalFiles(String folder) throws IOException {
        Path folderPath = Paths.get(folder);
        if (!exists(folderPath)) {
            LOGGER.warn("The asset folder {} does not exist.", folderPath.toAbsolutePath());
            return emptySet();
        }
        if (!isDirectory(folderPath)) {
            throw new IllegalArgumentException("The asset folder " + folderPath.toAbsolutePath() + "is not a directory.");
        }

        Set<URL> files = new HashSet<>();
        newDirectoryStream(folderPath).forEach(path -> files.add(pathToURL(path)));
        return files;
    }

    private static URL pathToURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new ApplicationInitializationException("Invalid file URL", e);
        }
    }

    private static String urlToUri(URL url) {
        try {
            return url.toURI().toASCIIString();
        } catch (URISyntaxException e) {
            throw new ApplicationInitializationException("Error while analyzing webpack asset url.", e);
        }
    }

    private static class WebpackAssets implements Assets {

        private static final String DEFAULT_ROOT_ELEMENT_ID = "app";

        private final Set<URL> assets;
        private final String rootElementId;

        public WebpackAssets(Set<URL> assets) {
            this(assets, DEFAULT_ROOT_ELEMENT_ID);
        }

        public WebpackAssets(Set<URL> assets, String rootElementId) {
            this.rootElementId = rootElementId;
            this.assets = assets;
        }

        @Override
        public String getMainCss() {
            return this.filterAssets(uri -> uri.endsWith("css"), "CSS");

        }

        @Override
        public String getMainJavascript() {
            return this.filterAssets(uri -> uri.endsWith("js"), "Javascript");
        }

        @Override
        public String getRootElementId() {
            return this.rootElementId;
        }

        @Override
        public boolean isEmpty() {
            return this.assets.isEmpty();
        }

        private String filterAssets(Predicate<String> filter, String type) {
            List<String> filteredAssets = this.assets
                .stream()
                .map(WebpackAssetsUtils::urlToUri)
                .filter(filter)
                .map(WebpackAssetsUtils::extractFilename)
                .collect(Collectors.toList());

            if (filteredAssets.size() > 1 || filteredAssets.isEmpty()) {
                throw new ApplicationInitializationException(
                    "One main " + type + " asset is expected but found " + filteredAssets.size() + ".");
            }

            return filteredAssets.get(0);
        }
    }
}
