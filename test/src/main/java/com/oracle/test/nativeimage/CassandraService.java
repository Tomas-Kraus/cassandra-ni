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
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
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

// PERF: Cassandra API is blocking so it's direct usage in Service request handlers
//       is not correct. But it will work for the purpose of simple jUnit testing.
/**
 * Cassandra database web service.
 */
public class CassandraService implements Service {

    private final CqlSession session;
    private final Map<String,String> statements;

    /**
     * Creates an instance of common web service code for testing application.
     *
     * @param session Cassandra database session
     * @param statements configured statements
     */
    public CassandraService(final CqlSession session, final Map<String,String> statements) {
        this.session = session;
        this.statements = statements;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/ping", this::ping)
                .get("/select", this::select)
                .get("/verify", this::verify)
                .get("/insert", this::insert)
                .get("/update", this::update)
                .get("/delete", this::delete);
    }

    // Returns Cassandra database version.
    private void ping(final ServerRequest request, final ServerResponse response) {
        ResultSet rs = session.execute(statements.get("ping"));
        Row row = rs.one();
        if (row != null) {
            JsonValue hw = Json.createValue(row.getString("release_version"));
            response.send(okStatus(hw));
        } else {
            response.send(exceptionStatus(
                    new RemoteTestException("No Cassandra version was returned")));
        }
    }

    // Select row from database table
    private void select(final ServerRequest request, final ServerResponse response) {
        try {
            int id = Integer.parseInt(param(request, "id"));
            ResultSet rs = session.execute(session
                    .prepare(statements.get("select"))
                    .bind(id));
            Row row = rs.one();
            if (row == null) {
                response.send(exceptionStatus(
                        new RemoteTestException("Test select failed: No rows returned.")));
            } else {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("id", row.getInt("id"));
                job.add("name", row.getString("name"));
                job.add("type", row.getString("type"));
                response.send(okStatus(job.build()));
            }
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Test select failed: %s", t.getMessage()))));
        }
    }

    // Verify row in database table (same as select but empty row is valid response).
    private void verify(final ServerRequest request, final ServerResponse response) {
        try {
            int id = Integer.parseInt(param(request, "id"));
            ResultSet rs = session.execute(session
                    .prepare(statements.get("select"))
                    .bind(id));
            Row row = rs.one();
            if (row == null) {
                response.send(okStatus(JsonValue.NULL));
            } else {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("id", row.getInt("id"));
                job.add("name", row.getString("name"));
                job.add("type", row.getString("type"));
                response.send(okStatus(job.build()));
            }
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Pokemon verification failed: %s", t.getMessage()))));
        }
    }

    // Insert row into database table
    private void insert(final ServerRequest request, final ServerResponse response) {
        try {
            int id = Integer.parseInt(param(request, "id"));
            String name = param(request, "name");
            String type = param(request, "type");
            session.execute(session
                    .prepare(statements.get("insert"))
                    .bind(id, name, type));
            response.send(okStatus(JsonValue.NULL));
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Test insert failed: %s", t.getMessage()))));
        }
    }

    // Update row in database table
    private void update(final ServerRequest request, final ServerResponse response) {
        try {
            int id = Integer.parseInt(param(request, "id"));
            String name = param(request, "name");
            session.execute(session
                    .prepare(statements.get("update"))
                    .bind(name, id));
            response.send(okStatus(JsonValue.NULL));
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Test update failed: %s", t.getMessage()))));
        }
    }

    // Delete row from database table
    private void delete(final ServerRequest request, final ServerResponse response) {
        try {
            int id = Integer.parseInt(param(request, "id"));
            session.execute(session
                    .prepare(statements.get("delete"))
                    .bind(id));
            response.send(okStatus(JsonValue.NULL));
        } catch (Throwable t) {
            response.send(exceptionStatus(
                    new RemoteTestException(String.format("Test delete failed: %s", t.getMessage()))));
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
