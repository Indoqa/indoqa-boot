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

import static java.lang.Thread.currentThread;
import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import spark.Response;

public class HeapDumpResources extends AbstractActuatorResources {

    private static final int HTTP_SC_TOO_MANY_REQUESTS = 429;
    private final long timeout = SECONDS.toMillis(30);
    private final Lock lock = new ReentrantLock();

    private HeapDumper heapDumper;

    private static File createTempFile(boolean live) throws IOException {
        String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm", US).format(new Date());
        File file = File.createTempFile("heapdump" + date + (live ? "-live" : ""), ".hprof");
        file.delete();
        return file;
    }

    public Object invokeHeapDump(Response res) throws IOException {
        try {
            if (this.lock.tryLock(this.timeout, MILLISECONDS)) {
                try {
                    return this.dumpHeap(true, res);
                } finally {
                    this.lock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            currentThread().interrupt();
        }
        res.status(HTTP_SC_TOO_MANY_REQUESTS);
        return EMPTY;
    }

    @PostConstruct
    public void mount() {
        if (this.isAdminServiceAvailable()) {
            this.getSparkAdminService().get("/heap-dump", (req, res) -> this.invokeHeapDump(res));
        }
    }

    /**
     * Factory method used to create the {@link HeapDumper}.
     * 
     * @return the heap dumper to use
     * @throws HeapDumperUnavailableException if the heap dumper cannot be created
     */
    protected HeapDumper createHeapDumper() throws HeapDumperUnavailableException {
        return new HotSpotDiagnosticMXBeanHeapDumper();
    }

    private InputStream dumpHeap(boolean live, Response res) throws IOException, InterruptedException {
        if (this.heapDumper == null) {
            this.heapDumper = this.createHeapDumper();
        }
        File file = createTempFile(live);
        try {
            this.heapDumper.dumpHeap(file, live);

            res.type("application/octet-stream");
            res.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return new FileInputStream(file);
        } finally {
            file.delete();
        }
    }

    /**
     * Strategy interface used to dump the heap to a file.
     */
    @FunctionalInterface
    protected interface HeapDumper {

        /**
         * Dump the current heap to the specified file.
         * 
         * @param file the file to dump the heap to
         * @param live if only <em>live</em> objects (i.e. objects that are reachable from others) should be dumped
         * @throws IOException on IO error
         * @throws InterruptedException on thread interruption
         */
        void dumpHeap(File file, boolean live) throws IOException, InterruptedException;

    }

    /**
     * Exception to be thrown if the {@link HeapDumper} cannot be created.
     */
    protected static class HeapDumperUnavailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public HeapDumperUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * {@link HeapDumper} that uses {@code com.sun.management.HotSpotDiagnosticMXBean} available on Oracle and OpenJDK to dump the heap
     * to a file.
     */
    protected static class HotSpotDiagnosticMXBeanHeapDumper implements HeapDumper {

        private Object diagnosticMXBean;

        private Method dumpHeapMethod;

        @SuppressWarnings("unchecked")
        protected HotSpotDiagnosticMXBeanHeapDumper() {
            try {
                Class<?> diagnosticMXBeanClass = ClassUtils.resolveClassName("com.sun.management.HotSpotDiagnosticMXBean", null);
                this.diagnosticMXBean = ManagementFactory.getPlatformMXBean((Class<PlatformManagedObject>) diagnosticMXBeanClass);
                this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass, "dumpHeap", String.class, Boolean.TYPE);
            } catch (Throwable ex) {
                throw new HeapDumperUnavailableException("Unable to locate HotSpotDiagnosticMXBean", ex);
            }
        }

        @Override
        public void dumpHeap(File file, boolean live) {
            ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean, file.getAbsolutePath(), live);
        }
    }
}
