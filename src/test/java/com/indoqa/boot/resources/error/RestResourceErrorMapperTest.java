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

import static com.indoqa.boot.resources.exception.AbstractRestResourceException.ErrorData;
import static com.indoqa.boot.resources.exception.HttpStatusCode.FORBIDDEN;
import static org.junit.Assert.*;

import com.indoqa.boot.resources.exception.AbstractRestResourceException;
import com.indoqa.boot.resources.exception.HttpStatusCode;
import org.junit.Before;
import org.junit.Test;

public class RestResourceErrorMapperTest {

    private RestResourceErrorMapper mapper;

    @Test
    public void testException1() {
        RestResourceError resourceError = this.mapper.buildError(new Exception1());
        assertEquals("e1", resourceError.getPayload());
    }

    @Test
    public void testException1a() {
        RestResourceError resourceError = this.mapper.buildError(new Exception1a());
        assertEquals("e1", resourceError.getPayload());
    }

    @Test
    public void testException2() {
        RestResourceError resourceError = this.mapper.buildError(new Exception2());
        assertEquals("e2", resourceError.getPayload());
    }

    @Test
    public void testException2a() {
        RestResourceError resourceError = this.mapper.buildError(new Exception2a());
        assertEquals("e2", resourceError.getPayload());
    }

    @Test
    public void testExceptionA() {
        RestResourceError resourceError = this.mapper.buildError(new ExceptionA());
        assertEquals("eA", resourceError.getPayload());
    }

    @Test
    public void testExceptionAa() {
        RestResourceError resourceError = this.mapper.buildError(new ExceptionAa());
        assertEquals("eA", resourceError.getPayload());
    }

    @Test
    public void testExceptionB() {
        RestResourceError resourceError = this.mapper.buildError(new ExceptionB());
        assertEquals("eB", resourceError.getPayload());
    }

    @Test
    public void testExceptionBa() {
        RestResourceError resourceError = this.mapper.buildError(new ExceptionBa());
        assertEquals("eB", resourceError.getPayload());
    }

    @Test
    public void testException() {
        RestResourceError resourceError = this.mapper.buildError(new Exception());
        assertEquals("Exception", resourceError.getError());
    }

    @Test
    public void testRuntimeException() {
        RestResourceError resourceError = this.mapper.buildError(new RuntimeException());
        assertEquals("RuntimeException", resourceError.getError());
    }

    @Test
    public void testCustomRestResourceException() {
        RestResourceError resourceError = this.mapper.buildError(new CustomRestResourceException("custom", FORBIDDEN, "custom-type"));
        assertEquals("custom", resourceError.getError());
        assertEquals(403, resourceError.getStatus());
        assertNotNull(resourceError.getPayload());
        assertNotNull(resourceError.getId());
        assertEquals(6, resourceError.getId().length());
        assertNotNull(resourceError.getTimestamp());

        assertTrue(ErrorData.class.equals(resourceError.getPayload().getClass()));
        ErrorData errorData = (ErrorData) resourceError.getPayload();
        assertEquals("custom-type", errorData.getType());
        assertEquals(1, errorData.getParameters().size());
        assertEquals("value-1", errorData.getParameters().get("param-1"));
    }

    @Before
    public void setUp() {
        this.mapper = new RestResourceErrorMapper(null);

        this.mapper.registerException(Exception1.class, e -> new RestResourceErrorInfo(null, "e1"));
        this.mapper.registerException(ExceptionB.class, e -> new RestResourceErrorInfo(null, "eB"));
        this.mapper.registerException(Exception2.class, e -> new RestResourceErrorInfo(null, "e2"));
        this.mapper.registerException(ExceptionA.class, e -> new RestResourceErrorInfo(null, "eA"));

        this.mapper.sortErrorProviders();
    }

    private static class Exception1 extends Exception {
        // nothing
    }

    private static class Exception1a extends Exception1 {
        // nothing
    }

    private static class Exception2 extends Exception1 {
        // nothing
    }

    private static class Exception2a extends Exception2 {
        // nothing
    }

    private static class ExceptionA extends RuntimeException {
        // nothing
    }

    private static class ExceptionAa extends ExceptionA {
        // nothing
    }

    private static class ExceptionB extends ExceptionA {
        // nothing
    }

    private static class ExceptionBa extends ExceptionB {
        // nothing
    }

    private static class CustomRestResourceException extends AbstractRestResourceException {

        public CustomRestResourceException(String message, HttpStatusCode statusCode, String type) {
            super(message, statusCode, type);
            this.setParameter("param-1", "value-1");
        }
    }
}
