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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests entity.
 */
public class Pokemon {

    static final Map<Integer, Pokemon> POKEMNONS = initPokemons();

    private final int id;
    private final String name;
    private final String type;

    Pokemon(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    int getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getType() {
        return type;
    }

    private static final Map<Integer, Pokemon> initPokemons() {
       Map<Integer, Pokemon> pokemons = new HashMap<>(3);
       pokemons.put(1, new Pokemon(1, "Bulbasaur", "grass"));
       pokemons.put(2, new Pokemon(2, "Charmander", "fire"));
       pokemons.put(3, new Pokemon(3, "Squirtle", "water"));
       return Collections.unmodifiableMap(pokemons);
    }

}
