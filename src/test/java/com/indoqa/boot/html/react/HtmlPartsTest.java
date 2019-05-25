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

import static org.junit.Assert.*;

import com.indoqa.boot.html.react.IndexHtmlBuilder.HtmlParts;
import org.junit.Test;

public class HtmlPartsTest {

    @Test
    public void findAllParts() {
        HtmlParts htmlParts = new HtmlParts(
            "<html>\n<head>foo<title>bar</title><meta name=\"a\" content=\"b\"/></head><body class=\"abc\"><h1>test</h1></body></html>");
        assertEquals("<html>\n", htmlParts.getBeforeHeadContent());
        assertEquals("<head>", htmlParts.getHeadElement());
        assertEquals("foo<meta name=\"a\" content=\"b\"/>", htmlParts.getHeadContent());
        assertEquals("<body class=\"abc\">", htmlParts.getBodyElement());
        assertEquals("<h1>test</h1>", htmlParts.getBodyContent());
        assertEquals("<h1>test</h1>", htmlParts.getBodyContent());
        assertEquals("<title>bar</title>", htmlParts.getTitle());
    }

    @Test
    public void findNoParts() {
        HtmlParts htmlParts = new HtmlParts("foo");
        assertEquals("<!DOCTYPE html>\n<html lang=\"de\">", htmlParts.getBeforeHeadContent());
        assertEquals("<head>", htmlParts.getHeadElement());
        assertNull(htmlParts.getHeadContent());
        assertEquals("<body>", htmlParts.getBodyElement());
        assertNull(htmlParts.getBodyContent());
        assertNull(htmlParts.getTitle());
    }

    @Test
    public void nullInput() {
        HtmlParts htmlParts = new HtmlParts(null);
        assertEquals("<!DOCTYPE html>\n<html lang=\"de\">", htmlParts.getBeforeHeadContent());
        assertEquals("<head>", htmlParts.getHeadElement());
        assertNull(htmlParts.getHeadContent());
        assertEquals("<body>", htmlParts.getBodyElement());
        assertNull(htmlParts.getBodyContent());
        assertNull(htmlParts.getTitle());
    }
}
