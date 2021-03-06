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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonValue;

import io.helidon.common.http.MediaType;
import io.helidon.tests.integration.tools.service.RemoteTestException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

import static io.helidon.tests.integration.tools.service.AppResponse.exceptionStatus;
import static io.helidon.tests.integration.tools.service.AppResponse.okStatus;

/**
 * Web resource to handle web server life cycle.
 */
public class LifeCycleService implements Service {

    private WebServer server;
    private final CqlSession session;
    private final Map<String,String> statements;

    /**
     * Creates an instance of web service to handle web server life cycle.
     *
     * @param session Cassandra database session
     * @param statements configured statements
     */
    public LifeCycleService(final CqlSession session, final Map<String,String> statements) {
        this.session = session;
        this.statements = statements;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/init", this::init)
                .get("/exit", this::exit);
    }

    public void setServer(final WebServer server) {
        this.server = server;
    }

    /**
     * Initializes database schema and content.
     *
     * @param request not used
     * @param response where to send server termination message.
     * @return {@code null} value
     */
    private void init(final ServerRequest request, final ServerResponse response) {
        try {
            session.execute(statements.get("create-keyspace"));
            session.execute(statements.get("use-keyspace"));
            session.execute(statements.get("create-table"));
            PreparedStatement ps = session.prepare(statements.get("insert"));
            Pokemon.POKEMNONS.forEach(
                    (id, pokemon) -> session.execute(ps.bind(id, pokemon.getName(), pokemon.getType())));
            response.send(okStatus(JsonValue.NULL));
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Could not initialize database: %s", t.getMessage()))));
        }
    }

    /**
     * Terminates web server.
     *
     * @param request not used
     * @param response where to send server termination message.
     * @return {@code null} value
     */
    private void exit(final ServerRequest request, final ServerResponse response) {
        session.close();
        response.headers().contentType(MediaType.TEXT_PLAIN);
        response.send("Testing web server shutting down.");
        ExitThread.start(server);
    }

    /**
     * Shut down web server after short delay.
     */
    private static final class ExitThread implements Runnable {

        private static final Logger LOGGER = Logger.getLogger(ExitThread.class.getName());

        /**
         * Starts application exit thread.
         *
         * @param server web server instance to shut down
         */
        public static final void start(final WebServer server) {
            new Thread(new ExitThread(server)).start();
        }

        private final WebServer server;

        private ExitThread(final WebServer server) {
            this.server = server;
        }

        /**
         * Wait few seconds and terminate web server.
         */
        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                LOGGER.log(Level.WARNING, ie, () -> String.format("Thread was interrupted: %s", ie.getMessage()));
            } finally {
                server.shutdown();
            }
        }

    }

}
