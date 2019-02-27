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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.json.transformer.HtmlEscapingAwareJsonTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import spark.Request;

public class IndexHtmlBuilder {

    private static final Logger LOGGER = getLogger(IndexHtmlBuilder.class);
    private static final String EMPTY_INITIAL_STATE = "{}";

    private final String classPathLocation;
    private final String fileSystemLocation;
    private final HtmlEscapingAwareJsonTransformer jsonTransformer;
    private final boolean isDev;

    private String indexHtmlPart1;
    private String indexHtmlPart2;

    IndexHtmlBuilder(String classPathLocation, String fileSystemLocation, HtmlEscapingAwareJsonTransformer jsonTransformer,
        boolean isDev) {
        this.classPathLocation = classPathLocation;
        this.fileSystemLocation = fileSystemLocation;
        this.jsonTransformer = jsonTransformer;
        this.isDev = isDev;
        splitIndexHtml(classPathLocation, fileSystemLocation);
    }

    public String html(Request request, InitialStateProvider initialStateProvider) {
        if (this.isDev) {
            splitIndexHtml(this.classPathLocation, this.fileSystemLocation);
        }
        return new StringBuilder()
            .append(this.indexHtmlPart1)
            .append("<script>window.__INITIAL_STATE__ = ")
            .append(this.createInitialStateJson(request, initialStateProvider))
            .append(";</script>")
            .append(this.indexHtmlPart2)
            .toString();
    }

    private void splitIndexHtml(String localClassPathLocation, String localFileSystemLocation) {
        String indexHtml = readIndexHtml(localClassPathLocation, localFileSystemLocation);
        int posFirstScript = indexHtml.indexOf("<script>");
        this.indexHtmlPart1 = indexHtml.substring(0, posFirstScript);
        this.indexHtmlPart2 = indexHtml.substring(posFirstScript);
    }

    private String createInitialStateJson(Request request, InitialStateProvider initialStateProvider) {
        String initialStateJson = EMPTY_INITIAL_STATE;
        if (initialStateProvider != null && this.jsonTransformer != null) {
            Object initialStateObject = initialStateProvider.initialState(request);
            if (initialStateObject != null) {
                initialStateJson = this.jsonTransformer.render(initialStateObject);
            }
        }
        return initialStateJson;
    }

    private String readIndexHtml(String localClassPathLocation, String localFileSystemLocation) {
        if (this.isDev) {
            File filesystemIndexHtml = new File(localFileSystemLocation, "index.html");

            if (!filesystemIndexHtml.exists()) {
                LOGGER.warn("There was no index.html found: " + filesystemIndexHtml.getAbsolutePath());
                return "<html><head><title>Error: index.html missing</title></head><body>Error: index.html missing</body></html>";
            }

            try {
                return FileUtils.readFileToString(filesystemIndexHtml, UTF_8);
            } catch (IOException e) {
                throw new ApplicationInitializationException(
                    "Error while reading index.html from the file system: " + filesystemIndexHtml.getAbsolutePath());
            }
        }
        String classpathIndexHtml = localClassPathLocation + "/index.html";
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream(classpathIndexHtml), UTF_8);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading index.html from classpath: " + classpathIndexHtml);
        }
    }
}
