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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;

import com.indoqa.boot.actuate.logging.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.LoggerContext;

import spark.Request;
import spark.Response;

public class Log4j2LoggingResource extends AbstractAdminResources {

    private static final String PATH_LOGGING = "/logging";
    private static final String PATH_LEVEL = PATH_LOGGING + "/level";
    private static final String PATH_MODIFICATIONS = PATH_LOGGING + "/modifications";
    private static final String PATH_RESET = PATH_LOGGING + "/reset";
    private static final String PATH_RESET_ALL = PATH_LOGGING + "/reset-all";

    private static final int MAX_SECONDS = 600;
    private static final int MIN_SECONDS = 1;

    private static final Level[] LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

    private final Map<String, RestoreTimerTask> restoreTasks = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(true);

    @PostConstruct
    public void mapResources() {
        this.getActuator(PATH_LEVEL, this::getLogLevel);
        this.getActuatorHtml(PATH_LEVEL, this::getLogLevel);

        this.getActuator(PATH_MODIFICATIONS, this::getModifications);
        this.getActuatorHtml(PATH_MODIFICATIONS, this::getModifications);

        this.putActuator(PATH_LEVEL, this::setLogLevel);
        this.putActuatorHtml(PATH_LEVEL, this::setLogLevel);

        this.putActuator(PATH_RESET, this::resetLogLevel);
        this.putActuatorHtml(PATH_RESET, this::resetLogLevel);

        this.putActuator(PATH_RESET_ALL, this::resetAllLogLevels);
        this.putActuatorHtml(PATH_RESET_ALL, this::resetAllLogLevels);
    }

    private ResetLoggers resetAllLogLevels(Request request, Response response) {
        if (this.restoreTasks.isEmpty()) {
            return ResetLoggers.nonReset();
        }

        ResetLoggers result = ResetLoggers.reset();

        for (RestoreTimerTask eachTask : this.restoreTasks.values()) {
            eachTask.execute();

            result.add(eachTask.getLoggerName(), eachTask.getLevelName());
        }

        return result;
    }

    private Logger getExistingLogger(String logger) {
        if (logger == null) {
            return null;
        }

        LoggerContext context = LogManager.getContext(false);
        if (context.hasLogger(logger)) {
            return context.getLogger(logger);
        }

        if (LogManager.exists(logger)) {
            return LogManager.getLogger(logger);
        }
        return null;
    }

    private ResetLogger resetLogLevel(Request request, Response response) {
        String loggerName = request.queryParams("logger");
        RestoreTimerTask restoreTimerTask = this.restoreTasks.get(loggerName);
        if (restoreTimerTask == null) {
            response.status(400);
            return ResetLogger.notReset(loggerName);
        }

        restoreTimerTask.execute();
        return ResetLogger.reset(loggerName, restoreTimerTask.getLevelName());
    }

    private ModifiedLogger setLogLevel(Request request, Response response) {
        String loggerName = request.queryParams("logger");
        String level = request.queryParams("level");
        int seconds = Integer.parseInt(request.queryParamOrDefault("seconds", "60"));

        if (seconds < MIN_SECONDS) {
            response.status(400);
            response.body(
                "Invalid value for parameter 'seconds': " + seconds + ". Use " + MIN_SECONDS + " <= seconds <= " + MAX_SECONDS + ".");
            return null;
        }

        if (seconds > MAX_SECONDS) {
            response.status(400);
            response.body(
                "Invalid value for parameter 'seconds': " + seconds + ". Use " + MIN_SECONDS + " <= seconds <= " + MAX_SECONDS + ".");
            return null;
        }

        Level actualLevel = Level.getLevel(level);
        if (actualLevel == null) {
            response.status(400);
            response.body("Unknown level '" + level + "'. Use one of " + Arrays.toString(LEVELS));
            return null;
        }

        Logger logger = getExistingLogger(loggerName);
        if (logger == null) {
            response.status(400);
            response.body("Logger '" + loggerName + "' does not exist.");
            return null;
        }

        Level originalLevel;
        RestoreTimerTask restoreTimerTask = this.restoreTasks.get(logger.getName());
        if (restoreTimerTask != null) {
            restoreTimerTask.cancel();
            originalLevel = restoreTimerTask.getLevel();
        } else {
            originalLevel = logger.getLevel();
        }

        if (actualLevel.equals(originalLevel)) {
            response.status(400);
            response.body("Cannot modify logger '" + loggerName + "' to its original log level. Use reset instead.");
            return null;
        }

        String modificationKey = UUID.randomUUID().toString();

        long delay = SECONDS.toMillis(seconds);
        restoreTimerTask = new RestoreTimerTask(logger.getName(), originalLevel, System.currentTimeMillis() + delay, modificationKey);
        this.timer.schedule(restoreTimerTask, delay);

        this.restoreTasks.put(logger.getName(), restoreTimerTask);

        Configurator.setLevel(logger.getName(), actualLevel);
        logger.error("Modified log level to '{}' via LoggingResource. Modification-Key: {}", level, modificationKey);

        return ModifiedLogger.of(logger.getName(),
            originalLevel.name(),
            level,
            restoreTimerTask.getExecutionTime(),
            Duration.ofMillis(restoreTimerTask.getRemainingTime()),
            modificationKey);
    }

    private ModifiedLoggers getModifications(Request request, Response response) {
        if (this.restoreTasks.isEmpty()) {
            return ModifiedLoggers.nonModified();
        }

        ModifiedLoggers modified = ModifiedLoggers.modified();
        for (RestoreTimerTask eachTask : this.restoreTasks.values()) {
            String currentLevel = getExistingLogger(eachTask.getLoggerName()).getLevel().name();
            Duration duration = Duration.ofMillis(eachTask.getRemainingTime());
            modified.add(eachTask.getLoggerName(),
                eachTask.getLevelName(),
                currentLevel,
                eachTask.getExecutionTime(),
                duration,
                eachTask.getModificationKey());
        }

        return modified;
    }

    private LogLevelResponse getLogLevel(Request request, Response response) {
        String loggerName = request.queryParams("logger");
        if (loggerName == null) {
            response.status(400);
            response.body("Query parameter 'logger' not set.");
            return null;
        }
        Logger existingLogger = this.getExistingLogger(loggerName);
        if (existingLogger == null) {
            response.status(400);
            return LogLevelResponse.notExising(loggerName);
        }
        RestoreTimerTask restoreTimerTask = this.restoreTasks.get(existingLogger.getName());
        if (restoreTimerTask != null) {
            return LogLevelResponse.modified(loggerName, restoreTimerTask.getLevel().name());
        }

        return LogLevelResponse.original(existingLogger.getName(), existingLogger.getLevel().name());
    }

    protected synchronized void resetLogLevel(RestoreTimerTask restoreTimerTask) {
        Logger logger = this.getExistingLogger(restoreTimerTask.getLoggerName());
        Configurator.setLevel(logger.getName(), restoreTimerTask.getLevel());
        this.restoreTasks.remove(restoreTimerTask.getLoggerName());

        logger.error("Restored log level to '{}' via LoggingResource. Modification-Key: {}",
            restoreTimerTask.getLevelName(),
            restoreTimerTask.getModificationKey());
    }

    private class RestoreTimerTask extends TimerTask {

        private final String loggerName;
        private final Level level;
        private final long executionTime;
        private final String modificationKey;

        public RestoreTimerTask(String loggerName, Level level, long executionTime, String modificationKey) {
            super();

            this.loggerName = loggerName;
            this.level = level;
            this.executionTime = executionTime;
            this.modificationKey = modificationKey;
        }

        public long getExecutionTime() {
            return this.executionTime;
        }

        public Level getLevel() {
            return this.level;
        }

        public String getLevelName() {
            if (this.level == null) {
                return "Inherited";
            }

            return this.level.toString();
        }

        public String getLoggerName() {
            return this.loggerName;
        }

        public String getModificationKey() {
            return this.modificationKey;
        }

        public long getRemainingTime() {
            return this.executionTime - System.currentTimeMillis();
        }

        @Override
        public void run() {
            this.execute();
        }

        protected void execute() {
            Log4j2LoggingResource.this.resetLogLevel(this);
        }
    }
}
