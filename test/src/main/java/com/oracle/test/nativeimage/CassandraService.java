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

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonValue;

import io.helidon.tests.integration.tools.service.RemoteTestException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

import static io.helidon.tests.integration.tools.service.AppResponse.exceptionStatus;
import static io.helidon.tests.integration.tools.service.AppResponse.okStatus;


/**
 * Cassandra database web service.
 */
public class CassandraService implements Service {

    private final CqlSession session;

    /**
     * Creates an instance of common web service code for testing application.
     *
     * @param session Cassandra database session
     */
    public CassandraService(final CqlSession session) {
        this.session = session;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/ping", this::ping)
                .get("/verifyHello", this::verifyHello);
    }

    // Returns Cassandra database version.
    private void ping(final ServerRequest request, final ServerResponse response) {
        ResultSet rs = session.execute("SELECT release_version FROM system.local");
        Row row = rs.one();
        if (row != null) {
            JsonValue hw = Json.createValue(row.getString("release_version"));
            response.send(okStatus(hw));
        } else {
            response.send(exceptionStatus(
                    new RemoteTestException("No Cassandra version was returned")));
        }
    }

    // Check whether provided HTTP query parameter "value" contains word "hello".
    private void verifyHello(final ServerRequest request, final ServerResponse response) {
        String value = param(request, "value");
        if (value.toLowerCase().contains("hello")) {
            response.send(okStatus(JsonValue.NULL));
        } else {
            response.send(
                    exceptionStatus(
                            new RemoteTestException(
                                    String.format("Value \"%s\" does not contain string \"hello\"", value))));
        }
    }

    /*
     * Retrieve HTTP query parameter value from request.
     *
     * @param request HTTP request context
     * @param name query parameter name
     * @return query parameter value
     * @throws RemoteTestException when no parameter with given name exists in request
     */
    private static String param( final ServerRequest request, final String name) {
        Optional<String> maybeParam = request.queryParams().first(name);
        if (maybeParam.isPresent()) {
            return maybeParam.get();
        } else {
            throw new RemoteTestException(String.format("Query parameter %s is missing.", name));
        }
    }

}
