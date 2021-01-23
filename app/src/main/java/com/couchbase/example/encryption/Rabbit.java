/*
 * Copyright 2021 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.example.encryption;

import com.couchbase.client.java.encryption.annotation.Encrypted;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Objects.requireNonNull;

public class Rabbit {
  private final String name;
  private final String magicWord;

  // Be careful to import the Jackson annotations from the correct package.
  // The Couchbase Java SDK includes a repackaged version of Jackson
  // in the package "com.couchbase.client.core.deps.com.fasterxml.jackson".
  // If you add your own version of Jackson to the class path,
  // the SDK will use your version for data binding, so make sure
  // the annotations on your data classes come from "com.fasterxml.jackson".
  public Rabbit(@JsonProperty("name") String name,
                @JsonProperty("magicWord") String magicWord) {
    this.name = requireNonNull(name);

    // CAVEAT: Jackson may pass a null value to the constructor
    // and set the actual value later via reflection.
    this.magicWord = magicWord;
  }

  public String getName() {
    return name;
  }

  @Encrypted
  public String getMagicWord() {
    return magicWord;
  }

  @Override
  public String toString() {
    return name + " responds to '" + magicWord + "'";
  }
}
