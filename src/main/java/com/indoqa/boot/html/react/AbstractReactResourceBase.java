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

import static com.indoqa.boot.html.react.WebpackAssetsUtils.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.slf4j.LoggerFactory.getLogger;
import static spark.Spark.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.html.resources.HtmlResponseModifier;
import com.indoqa.boot.profile.ProfileDetector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import spark.Spark;

/**
 * Use this base implementation for React/Redux single-page applications that are build by Webpack.
 */
public abstract class AbstractReactResourceBase extends AbstractHtmlResourcesBase {

    private static final Logger LOGGER = getLogger(AbstractReactResourceBase.class);
    private static final long EXPIRE_TIME = DAYS.toSeconds(1000);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final String NAME_ASSET_MANIFEST_JSON = "asset-manifest.json";

    @Inject
    private Environment environment;

    private static boolean checkFileSystemLocation(Path fileSystemLocationPath) {
        if (!Files.exists(fileSystemLocationPath)) {
            LOGGER.error("The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " does not exist.");
            return false;
        }
        if (!Files.isDirectory(fileSystemLocationPath)) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " is not a directory.");
        }
        if (!Files.isReadable(fileSystemLocationPath)) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " is not readable.");
        }
        String[] files = fileSystemLocationPath.toFile().list();
        if (files == null || files.length == 0) {
            LOGGER.error("The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " does not contain any resources.");
            return false;
        }

        return true;
    }

    private static void configureClasspathAssets(String mountPath, String classPathLocation, ReactHtmlBuilder htmlBuilder) {
        Spark.staticFiles.expireTime(EXPIRE_TIME);

        staticFileLocation(classPathLocation);

        if (StringUtils.isBlank(classPathLocation)) {
            throw new ApplicationInitializationException("The classpath location is empty or null.");
        }

        String assetManifestPath = classPathLocation.substring(1) + "/" + NAME_ASSET_MANIFEST_JSON;
        URL assetManifestUrl = AbstractReactResourceBase.class.getClassLoader().getResource(assetManifestPath);
        if (assetManifestUrl != null) {
            LOGGER.info("Found asset manifest in the classpath: {}", assetManifestPath);
            findWebpackAssetsByManifest(htmlBuilder, assetManifestUrl);
        }
        else {
            LOGGER.warn("There is no asset manifest in the classpath. Falling back to classpath detection.");
            findWebpackAssetsInClasspath(mountPath,
                classPathLocation,
                htmlBuilder::setMainCssPath,
                htmlBuilder::setMainJavascriptPath
            );
        }
    }

    private static void findWebpackAssetsByManifest(ReactHtmlBuilder htmlBuilder, URL assetManifestUrl) {
        try (InputStream assetManifest = assetManifestUrl.openStream()) {
            try (JsonParser jp = JSON_FACTORY.createParser(assetManifest)) {
                Map<String, String> artifacts = getArtifacts(jp);
                if (artifacts.isEmpty()) {
                    throw new ApplicationInitializationException("The asset manifest in {} is empty: " + assetManifestUrl.getPath());
                }
                findMainJavascriptAsset(htmlBuilder, artifacts);
                findMainCSSAsset(htmlBuilder, artifacts);
            }
        } catch (IOException ioe) {
            throw new ApplicationInitializationException("Error while reading the " + NAME_ASSET_MANIFEST_JSON + ".", ioe);
        }
    }

    private static void findMainJavascriptAsset(ReactHtmlBuilder htmlBuilder, Map<String, String> artifacts) {
        artifacts
            .keySet()
            .stream()
            .filter(artifactName -> artifactName.length() > 0)
            .filter(artifactName -> !isNumeric(artifactName.substring(0, 1)))
            .filter(artifactName -> !artifactName.startsWith("vendors"))
            .filter(artifactName -> !artifactName.startsWith("~runtime"))
            .filter(artifactName -> artifactName.endsWith(".js"))
            .findFirst()
            .ifPresent(artifactKey -> {
                String artifactPath = artifacts.get(artifactKey);
                LOGGER.info("Found root javascript entry: {}:{}", artifactKey, artifactPath);
                htmlBuilder.setMainJavascriptPath(artifactPath);
            });
    }

    private static void findMainCSSAsset(ReactHtmlBuilder htmlBuilder, Map<String, String> artifacts) {
        artifacts
            .keySet()
            .stream()
            .filter(artifactName -> artifactName.length() > 0)
            .filter(artifactName -> !isNumeric(artifactName.substring(0, 1)))
            .filter(artifactName -> artifactName.endsWith(".css"))
            .findFirst()
            .ifPresent(artifactKey -> {
                String artifactPath = artifacts.get(artifactKey);
                LOGGER.info("Found root CSS entry: {}:{}", artifactKey, artifactPath);
                htmlBuilder.setMainCssPath(artifactPath);
            });
    }

    private static void configureFileSystemAssets(String mountPath, String fileSystemLocation, ReactHtmlBuilder htmlBuilder) {
        String assetsFolder = getAssetsFolder(fileSystemLocation, mountPath);
        Path assetsFolderPath = Paths.get(assetsFolder);
        boolean localResourcesAvailable = checkFileSystemLocation(assetsFolderPath);

        if (!localResourcesAvailable) {
            return;
        }

        externalStaticFileLocation(fileSystemLocation);

        Path assetManifestPath = assetsFolderPath.getParent().resolve(NAME_ASSET_MANIFEST_JSON);
        if (Files.exists(assetManifestPath)) {
            LOGGER.info("Found asset manifest in the filesystem: {}", assetManifestPath);
            findWebpackAssetsByManifest(htmlBuilder, toUrl(assetManifestPath));
        }
        else {
            LOGGER.warn("There is no asset manifest in the filesystem at {}. Falling back to filesystem search.", assetManifestPath);
            findWebpackAssetsInFilesystem(assetsFolder,
                fileSystemLocation,
                htmlBuilder::setMainCssPath,
                htmlBuilder::setMainJavascriptPath
            );
        }
    }

    private static URL toUrl(Path assetManifestPath) {
        try {
            return assetManifestPath.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new ApplicationInitializationException("Error while translating the path " + assetManifestPath + " to an URL.");
        }
    }

    private static Map<String, String> getArtifacts(JsonParser jp) throws IOException {
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }
        Map<String, String> artifacts = new ConcurrentHashMap<>();
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            artifacts.put(fieldName, jp.getValueAsString());
        }
        return artifacts;
    }

    protected void configureHtmlBuilder(ReactHtmlBuilder reactHtmlBuilder) {
        // default does nothing
    }

    protected ReactHtmlBuilder createHtmlBuilder() {
        return new ReactHtmlBuilder();
    }

    protected void html(String mountPath, String classPathLocation, String fileSystemLocation) {
        this.html(mountPath, classPathLocation, fileSystemLocation, null);
    }

    protected void html(String mountPath, String classPathLocation, String fileSystemLocation, HtmlResponseModifier responseModifier) {
        ReactHtmlBuilder htmlBuilder = this.createHtmlBuilder();

        if (ProfileDetector.isDev(this.environment)) {
            configureFileSystemAssets(mountPath, fileSystemLocation, htmlBuilder);
        }
        else {
            configureClasspathAssets(mountPath, classPathLocation, htmlBuilder);
        }

        this.configureHtmlBuilder(htmlBuilder);

        this.html(mountPath + "/*", htmlBuilder, responseModifier);
    }
}
