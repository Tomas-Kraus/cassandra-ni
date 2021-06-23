/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oracle.test.nativeimage;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.tests.integration.tools.client.HelidonProcessRunner;
import io.helidon.tests.integration.tools.client.TestClient;
import io.helidon.tests.integration.tools.client.TestServiceClient;
import io.helidon.tests.integration.tools.client.TestsLifeCycleExtension;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.CqlSession;

// Implements global setup and close actions. This class must be registered as SPI service.
/**
 * jUnit test life cycle extensions.
 */
public class ServerLifeCycleExtension extends TestsLifeCycleExtension {

    private static final Logger LOGGER = Logger.getLogger(ServerLifeCycleExtension.class.getName());

    // Application config file retrieved from "app.config" property
    private final String appConfigProperty;
    // Whether application is build as native image ("native.image" property).
    private final Boolean nativeImage;
    // HTTP client will be initialized after web server startup.
    private TestServiceClient lifeCycleClient = null;

    public ServerLifeCycleExtension() {
        appConfigProperty = System.getProperty("app.config");
        nativeImage = Boolean.valueOf(System.getProperty("native.image", "false"));
    }


    @Override
    public void check() {
        LOGGER.info("Running initial test check()");
        try {
            waitForDatabase();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, t, () -> String.format("Database check failed: %s", t.getMessage()));
            throw t;
        }
    }

    /**
     * Setup application tests.
     */
    @Override
    public void setup() {
        lifeCycleClient = TestClient.builder()
                    .port(HelidonProcessRunner.HTTP_PORT)
                    .service("LifeCycle")
                    .build();
        LOGGER.info("Running global test setup setup()");
        lifeCycleClient.callServiceAndGetData("init");
    }

    /**
     * Cleanup JPA application tests.
     * @throws java.lang.Throwable
     */
    @Override
    public void close() throws Throwable {
        LOGGER.fine("Running global test close()");
        runner.stopApplication();
    }

    @Override
    protected HelidonProcessRunner.ExecType processRunnerExecType() {
        return nativeImage ? HelidonProcessRunner.ExecType.NATIVE : HelidonProcessRunner.ExecType.CLASS_PATH;
    }

    @Override
    protected String processRunnerModuleName() {
        return "com.oracle.test.nativeimage";
    }

    @Override
    protected String processRunnerMainClass() {
        return ServerMain.class.getName();
    }

    @Override
    protected String processRunnerFinalName() {
        return "native-image-test";
    }

    @Override
    protected String[] processRunnerArgs() {
        LOGGER.info(String.format("processRunnerArgs: appConfigProperty=%s", appConfigProperty));
        return new String[] {appConfigProperty};
    }

    @Override
    protected Runnable processRunnerStopCommand() {
        return () -> {
            String response = lifeCycleClient.callServiceAndGetString("exit");
            LOGGER.fine(() -> String.format("Response: %s", response));
        };
    }

    // Thread sleep time in miliseconds while waiting for database or appserver to come up.
    private static final int SLEEP_MILIS = 250;

    // Startup timeout in seconds for database server.
    private static final int TIMEOUT = 60;

    @SuppressWarnings("SleepWhileInLoop")
    public static void waitForDatabase() {
        final String appConfig = System.getProperty("app.config");
        final Config config = Config.create(ConfigSources.classpath(appConfig));
        final long start = System.currentTimeMillis();
        final String host = config.get("db.connection.host").as(String.class).get();
        final int port = config.get("db.connection.port").as(Integer.class).get();
        long endTm = 1000 * TIMEOUT + System.currentTimeMillis();
        while (true) {
            try {
                final CqlSession session = CqlSession.builder()
                        .addContactPoint(new InetSocketAddress(host, port))
                        .withLocalDatacenter("single")
                        .build();
                LOGGER.info(() -> String.format("Database is running at %s:%d", host, port));
                session.close();
                return;
            } catch (AllNodesFailedException ex) {
                LOGGER.finest(() -> String.format("Connection failed: %s", ex.getMessage()));
                if (System.currentTimeMillis() > endTm) {
                    throw new IllegalStateException(String.format("Database is not ready within %d seconds", TIMEOUT));
                }
                try {
                    Thread.sleep(SLEEP_MILIS);
                } catch (InterruptedException ie) {
                    LOGGER.warning(() -> String.format("Thread was interrupted: %s", ie.getMessage()));
                }
            }
        }
    }

    // Close database connection.
    private static void closeConnection(final Connection connection) {
        try {
            connection.close();
        } catch (SQLException ex) {
            LOGGER.warning(() -> String.format("Could not close database connection: %s", ex.getMessage()));
        }
    }

}
