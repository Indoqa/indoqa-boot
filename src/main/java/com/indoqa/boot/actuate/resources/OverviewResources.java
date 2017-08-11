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
package com.indoqa.boot.actuate.resources;

import static com.indoqa.boot.profile.ProfileDetector.isDev;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Date;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.core.env.Environment;

import com.indoqa.boot.actuate.systeminfo.SystemInfo;

public class OverviewResources extends AbstractActuatorResources {

    private static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";

    @Inject
    private SystemInfo systemInfo;

    @Inject
    private Environment environment;

    private static StringBuilder createDownloadLinkItem(String name, String link) {
        return new StringBuilder()
            .append("<li>")
            .append("<a href=\"")
            .append(link)
            .append("\">")
            .append(name)
            .append("</a>")
            .append("</li>");
    }

    private static StringBuilder createLinkItem(String name, String link) {
        return new StringBuilder()
            .append("<li>")
            .append("<a href=\"")
            .append(link)
            .append("\" target=\"_blank\">")
            .append(name)
            .append("</a>")
            .append("</li>");
    }

    private static CharSequence createLinkItem(String name, String link, Supplier<Boolean> predicate) {
        if (predicate.get()) {
            return createLinkItem(name, link);
        }
        return EMPTY;
    }

    private static StringBuilder printCss() {
        return new StringBuilder()
            .append("<style>")
            .append("body {")
            .append("  background-color: #000033;")
            .append("  color: #fff;")
            .append("  font-family: monospace;")
            .append("  font-size: 15px;")
            .append("  margin: 5px;")
            .append("}")
            .append("a, a:link, a:active, a:visited, a:hover {")
            .append("  color: #fff;")
            .append("}")
            .append("ul {")
            .append("  list-style-type: square;")
            .append("}")
            .append("</style>");
    }

    private static StringBuilder printHeader(SystemInfo systemInfo) {
        String asciiLogo = systemInfo.getAsciiLogo();

        StringBuilder headerBuilder = new StringBuilder();
        if (isBlank(asciiLogo)) {
            headerBuilder.append("<h1>").append(systemInfo.getApplicationName()).append("</h1>");
        } else {
            headerBuilder.append("<pre>").append(asciiLogo).append("</pre>");
        }
        return headerBuilder
            .append("<p><small>")
            .append("version: ")
            .append(systemInfo.getVersion())
            .append(", hostname: ")
            .append(systemInfo.getHostname())
            .append(", active profile(s): ")
            .append(join("|", asList(systemInfo.getProfiles())))
            .append("</small></p>");
    }

    private static String sendOverviewPage(SystemInfo systemInfo, Environment environment) {
        return new StringBuilder()
            .append("<!DOCTYPE html><html><head>")
            .append("<meta http-equiv=\"")
            .append(RESPONSE_HEADER_CONTENT_TYPE)
            .append("\" content=\"")
            .append(CONTENT_TYPE_HTML)
            .append("\">")
            .append(printCss())
            .append("<title>")
            .append(systemInfo.getApplicationName())
            .append(" :: Admin")
            .append("</title>")
            .append("</head>")
            .append("<body>")
            .append(printHeader(systemInfo))
            .append("<ul>")
            .append(createLinkItem("Application home", "http://localhost:" + systemInfo.getPort(), () -> isDev(environment)))
            .append(createLinkItem("System info", "./system-info"))
            .append(createLinkItem("Spring beans", "./spring-beans"))
            .append(createLinkItem("Health checks", "./health"))
            .append(createLinkItem("Metrics", "./metrics"))
            .append(createLinkItem("Thread dump", "./thread-dump"))
            .append(createDownloadLinkItem("Heap dump", "./heap-dump"))
            .append("</ul>")
            .append("<br/><small>created at: ")
            .append(new Date())
            .append("</small>")
            .append("</body></html>")
            .toString();
    }

    @PostConstruct
    public void mount() {
        if (this.isAdminServiceAvailable()) {
            this.getSparkAdminService().get("/", CONTENT_TYPE_HTML, (req, res) -> sendOverviewPage(this.systemInfo, this.environment));
        }
    }
}
