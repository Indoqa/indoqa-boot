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
package com.indoqa.boot.json;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class HtmlEscapingJacksonTransformer extends JacksonTransformer implements HtmlEscapingAwareJsonTransformer {

    @Override
    protected void configure() {
        super.configure();
        this.objectMapper.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
    }

    public class HTMLCharacterEscapes extends CharacterEscapes {

        private static final long serialVersionUID = 1L;
        private final int[] asciiEscapes;

        public HTMLCharacterEscapes() {
            int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
            esc['<'] = CharacterEscapes.ESCAPE_CUSTOM;
            esc['>'] = CharacterEscapes.ESCAPE_CUSTOM;
            this.asciiEscapes = esc;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return this.asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            if (ch == '<') {
                return new SerializedString("&lt;");
            }
            if (ch == '>') {
                return new SerializedString("&gt;");
            }

            return null;
        }
    }
}
