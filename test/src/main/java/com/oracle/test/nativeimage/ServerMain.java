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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Main Class.
 * Sample server application entry point.
 */
public class ServerMain {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    // Default configuration file name
    private static final String DEFAULT_CONFIG_FILE="test.yaml";

    /**
     * Main method.
     *
     * @param args command line arguments. 1st argument is configuration file.
     */
    public static void main(String[] args) {

        String configFile;
        if (args != null && args.length > 0) {
            configFile = args[0];
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }
        LOGGER.info(() -> String.format("Configuration file: %s", configFile));

        LogConfig.configureRuntime();
        startServer(configFile);

    }

    private static WebServer startServer(final String configFile) {

        final Config config = Config.create(ConfigSources.classpath(configFile));
        final Map<String,String> statements = statementsMap(config);
        final CqlSession session = CqlSession.builder()
                .addContactPoint(
                        new InetSocketAddress(
                                config.get("db.connection.host").as(String.class).get(),
                                config.get("db.connection.port").as(Integer.class).get()))
                .withLocalDatacenter("single")
                .build();
        final LifeCycleService lcResource = new LifeCycleService(session, statements);
        final Routing routing = Routing.builder()
                .register("/LifeCycle", lcResource)
                .register("/Cassandra", new CassandraService(session, statements))
                .build();

        final WebServer server = WebServer.builder()
                .routing(routing)
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .build();

        // Set server instance to exit resource.
        lcResource.setServer(server);
        // Start the server and print some info.
        server.start().thenAccept(
                ws -> {
                    System.out.println(String.format("WEB server is up! http://localhost:%d/", ws.port()));
                });

        // Server threads are not daemon. NO need to block. Just react.
        server.whenShutdown().thenRun(
                () -> System.out.println("WEB server is DOWN. Good bye!"));

        return server;
    }

    private static Map<String,String> statementsMap(final Config config) {
        final Config statementsConfig = config.get("db.statements");
        Map<String,String> statements = new HashMap<>();
        statementsConfig.traverse().forEach(node -> {
            if (node.isLeaf()) {
                statements.put(node.name(), node.asString().get());
            }
        });
        return statements;
    }
}
