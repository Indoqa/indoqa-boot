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
package com.indoqa.boot.resources.error;

import java.io.Serializable;
import java.time.Instant;

public class RestResourceError implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private int status;
    private Instant timestamp;
    private String error;
    private Object payload;

    private RestResourceError() {
        super();
    }

    public static RestResourceError build(int status, Instant timestamp, String id, String error) {
        return build(status, timestamp, id, error, null);
    }

    public static RestResourceError build(int status, Instant timestamp, String id, String error, Object payload) {
        RestResourceError result = new RestResourceError();
        result.setId(id);
        result.setStatus(status);
        result.setTimestamp(timestamp);
        result.setError(error);
        result.setPayload(payload);
        return result;
    }

    public String getError() {
        return this.error;
    }

    private void setError(String error) {
        this.error = error;
    }

    public Object getPayload() {
        return this.payload;
    }

    private void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    private void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return this.id;
    }

    private void setId(String id) {
        this.id = id;
    }
}
