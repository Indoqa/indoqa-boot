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

import static java.nio.file.Files.newDirectoryStream;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.spring.ClassPathScanner;

public final class WebpackAssetsUtils {

    private static final String ASSETS_PATH = "assets";
    private static final ClassPathScanner SCANNER = new ClassPathScanner();

    private WebpackAssetsUtils() {
        // hide utility class constructor
    }

    public static void findWebpackAssetsInClasspath(String mountPath, String folder, Consumer<String> setMainCss,
            Consumer<String> setMainJavascript) {
        try {
            Set<URL> resources = SCANNER.findFiles(createClasspathAssetsPattern(folder, mountPath));
            findFirstResource(resources, folder, ".css", setMainCss);
            findFirstResource(resources, folder, ".js", setMainJavascript);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while searching for webpack assets.", e);
        }
    }

    public static void findWebpackAssetsInFilesystem(String mountPath, String folder, Consumer<String> setMainCss,
            Consumer<String> setMainJavascript) {
        Set<URL> resources = findLocalFiles(getAssetsFolder(folder, mountPath));
        findFirstResource(resources, folder, ".css", setMainCss);
        findFirstResource(resources, folder, ".js", setMainJavascript);
    }

    private static String createClasspathAssetsPattern(String folder, String mountPath) {
        return getAssetsFolder(folder, mountPath) + "/*";
    }

    private static void findFirstResource(Set<URL> resources, String folder, String suffix, Consumer<String> consumer) {
        resources
            .stream()
            .map(url -> url.getPath())
            .filter(path -> path.endsWith(suffix))
            .findFirst()
            .map(path -> substringAfterLast(path, folder))
            .ifPresent(consumer);
    }

    private static Set<URL> findLocalFiles(String folder) {
        Set<URL> files = new HashSet<>();
        Path folderPath = Paths.get(folder);
        try (DirectoryStream<Path> directoryStream = newDirectoryStream(folderPath)) {
            for (Path eachPath : directoryStream) {
                files.add(pathToURL(eachPath));
            }
        } catch (IOException ex) {
            throw new ApplicationInitializationException("Error while accessing children of " + folderPath.toAbsolutePath());
        }
        return files;
    }

    private static String getAssetsFolder(String folder, String mountPath) {
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
}
