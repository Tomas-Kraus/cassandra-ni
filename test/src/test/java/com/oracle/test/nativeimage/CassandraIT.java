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
import java.util.logging.Logger;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import io.helidon.tests.integration.tools.client.HelidonProcessRunner;
import io.helidon.tests.integration.tools.client.HelidonTestException;
import io.helidon.tests.integration.tools.client.TestClient;
import io.helidon.tests.integration.tools.client.TestServiceClient;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test Cassandra service.
 */
public class CassandraIT {

    private static final Logger LOGGER = Logger.getLogger(CassandraIT.class.getName());

    private final TestServiceClient testClient = TestClient.builder()
            .port(HelidonProcessRunner.HTTP_PORT)
            .service("Cassandra")
            .build();

    // Test Cassandra ping statement.
    @Test
    public void testPing() {
        LOGGER.fine(() -> "Running testPing");
        try {
            JsonValue data = testClient.callServiceAndGetData("ping");
            LOGGER.info(() -> String.format("Cassandra version: %s", ((JsonString)data).getString()));
        } catch (HelidonTestException te) {
            fail(String.format(
                    "Caught %s: %s",
                    te.getClass().getSimpleName(),
                    te.getMessage()));
        }
        
    }

    // Test sSelect row from database table
    @Test
    void testSelect() {
        LOGGER.fine("Running testSelect");
        Pokemon pokemon = Pokemon.POKEMNONS.get(1);
        try {
            JsonValue dataValue = testClient
                    .callServiceAndGetData(
                            "select",
                            Map.of("id", String.valueOf(pokemon.getId())));
            assertThat(dataValue.getValueType(), equalTo(JsonValue.ValueType.OBJECT));
            JsonObject data = dataValue.asJsonObject();
            assertThat(pokemon.getId(), equalTo(data.getInt("id")));
            assertThat(pokemon.getName(), equalTo(data.getString("name")));
            assertThat(pokemon.getType(), equalTo(data.getString("type")));
        } catch (HelidonTestException te) {
            fail(String.format(
                    "Caught %s: %s",
                    te.getClass().getSimpleName(),
                    te.getMessage()));
        }
    }

    // Test insert row into database table
    @Test
    void testInsert() {
        LOGGER.fine("Running testInsert");
        try {
            JsonValue data = testClient.callServiceAndGetData(
                    "insert",
                    Map.of("id", "100", "name", "Pikachu", "type", "electric"));
            
        } catch (HelidonTestException te) {
            fail(String.format(
                    "Caught %s: %s",
                    te.getClass().getSimpleName(),
                    te.getMessage()));
        }
    }

}
