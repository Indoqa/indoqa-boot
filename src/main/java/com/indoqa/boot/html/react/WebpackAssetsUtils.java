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
package com.indoqa.boot.html.react;

import static java.nio.file.Files.*;
import static java.util.Collections.emptySet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.spring.ClassPathScanner;

public final class WebpackAssetsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebpackAssetsUtils.class);
    private static final String ASSETS_PATH = "assets";
    private static final ClassPathScanner SCANNER = new ClassPathScanner();

    public static void findWebpackAssetsInClasspath(String mountPath, String folder, Consumer<String> setMainCss,
            Consumer<String> setMainJavascript) {
        try {
            Set<URL> files = SCANNER.findFiles(createClasspathAssetsPattern(folder, mountPath));
            findFirstResource(files, folder, ".css", setMainCss);
            findFirstResource(files, folder, ".js", setMainJavascript);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while searching for webpack assets.", e);
        }
    }

    public static void findWebpackAssetsInFilesystem(String mountPath, String folder, Consumer<String> setMainCss,
            Consumer<String> setMainJavascript) {
        try {
            Set<URL> files = findLocalFiles(createFileSystemAssetsFolder(folder, mountPath));
            findFirstResource(files, folder, ".css", setMainCss);
            findFirstResource(files, folder, ".js", setMainJavascript);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while searching for webpack assets.", e);
        }
    }

    private static String createClasspathAssetsPattern(String folder, String mountPath) {
        return createFileSystemAssetsFolder(folder, mountPath) + "/*";
    }

    private static String createFileSystemAssetsFolder(String folder, String mountPath) {
        StringBuilder builder = new StringBuilder();

        builder.append(removeTrailingSlash(folder));

        if (!mountPath.startsWith("/")) {
            builder.append("/");
        }

        builder.append(removeTrailingSlash(mountPath));

        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append("/");
        }

        builder.append(ASSETS_PATH);

        return builder.toString();
    }

    private static void findFirstResource(Set<URL> findFiles, String folder, String suffix, Consumer<String> consumer) {
        findFiles.stream()
            .map(url -> url.getPath())
            .filter(path -> path.endsWith(suffix))
            .findFirst()
            .map(path -> StringUtils.substringAfter(path, folder))
            .ifPresent(consumer);
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

    private static String removeTrailingSlash(String path) {
        return path.replaceAll("/$", "");
    }

    private WebpackAssetsUtils() {
        // hide utility class constructor
    }
}
