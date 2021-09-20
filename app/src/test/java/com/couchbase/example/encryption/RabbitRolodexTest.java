/*
 * Copyright 2021 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.example.encryption;

import com.couchbase.client.core.encryption.CryptoManager;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.encryption.AeadAes256CbcHmacSha512Provider;
import com.couchbase.client.encryption.DefaultCryptoManager;
import com.couchbase.client.encryption.Keyring;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.codec.TypeRef;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.json.JsonObjectCrypto;
import com.couchbase.client.java.kv.MutateInSpec;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

import static com.couchbase.client.core.util.CbCollections.listOf;
import static com.couchbase.client.java.ClusterOptions.clusterOptions;
import static com.couchbase.client.java.diagnostics.WaitUntilReadyOptions.waitUntilReadyOptions;
import static java.util.Collections.singletonMap;

public class RabbitRolodexTest {

  private static final String COUCHBASE_CONNECTION_STRING = "localhost";
  private static final String COUCHBASE_USERNAME = "Administrator";
  private static final String COUCHBASE_PASSWORD = "password";
  private static final String COUCHBASE_BUCKET = "default";

  private static final String DOCUMENT_ID = "rolodex";

  private static final String ENCRYPTION_KEY_NAME = "myKey";

  private static final Keyring keyring = keyringWithRandomKey();
  private static final CryptoManager cryptoManager = simpleCryptoManager(keyring);

  private static ClusterEnvironment env;
  private static Cluster cluster;
  private static Collection collection;

  @BeforeAll
  static void connect() {
    env = ClusterEnvironment.builder()
        .cryptoManager(cryptoManager)
        .build();

    cluster = Cluster.connect(COUCHBASE_CONNECTION_STRING,
        clusterOptions(COUCHBASE_USERNAME, COUCHBASE_PASSWORD)
            .environment(env));

    // Open the bucket and grab a reference to the default collection
    Bucket bucket = cluster.bucket(COUCHBASE_BUCKET);
    bucket.waitUntilReady(Duration.ofSeconds(10), waitUntilReadyOptions()
        .serviceTypes(ServiceType.KV));

    collection = bucket.defaultCollection();
  }

  @AfterAll
  static void disconnect() {
    cluster.disconnect();
    env.shutdown();
  }

  private static Keyring keyringWithRandomKey() {
    byte[] keyBytes = new byte[64];
    new SecureRandom().nextBytes(keyBytes);

    return Keyring.fromMap(
        singletonMap(ENCRYPTION_KEY_NAME, keyBytes)
    );
  }

  private static Keyring filesystemRotatingKeyring() {
    return Keyring.caching(Duration.ofMinutes(1), 5000, new FilesystemRotatingKeyring(
        Paths.get("/tmp/secrets"),
        "--",
        ".key",
        ".key.primary"
    ));
  }

  private static CryptoManager simpleCryptoManager(Keyring keyring) {
    AeadAes256CbcHmacSha512Provider aes = AeadAes256CbcHmacSha512Provider.builder()
        .keyring(keyring)
        .build();

    return DefaultCryptoManager.builder()
        .decrypter(aes.decrypter())
        .defaultEncrypter(aes.encrypterForKey(ENCRYPTION_KEY_NAME))
        .build();
  }

  @Test
  void upsertRabbits() {
    collection.upsert(DOCUMENT_ID, listOf(
        new Rabbit("Flopsy", "Ajji Majji la Tarajji"),
        new Rabbit("Mopsy", "Alakazam")
    ));

    displayRolodex();
  }

  //@Test
  void appendRabbit() {
    collection.mutateIn(DOCUMENT_ID, listOf(
        MutateInSpec.arrayAppend("", listOf(
            new Rabbit("Fluffy", "Hocus Pocus")
        )))
    );

    displayRolodex();
  }

  private void displayRolodex() {
    System.out.println("Document in database:");
    displayEncrypted();

    System.out.println();
    System.out.println("Decrypted rolodex");
    System.out.println("=================");

    displayDecrypted();
  }

  void displayEncrypted() {
    String encrypted = collection.get(DOCUMENT_ID)
        .contentAs(JsonNode.class)
        .toPrettyString();

    System.out.println(encrypted);
  }

  void displayDecrypted() {
    List<Rabbit> decrypted = collection.get("rolodex")
        .contentAs(new TypeRef<List<Rabbit>>() {
        });

    decrypted.forEach(System.out::println);
  }

  // If data binding is not flexible enough for you use case
  void displayDecryptedUsingJsonObject() {
    JsonArray rabbits = collection.get("rolodex").contentAsArray();
    for (Object o : rabbits) {
      JsonObject rabbit = (JsonObject) o;
      JsonObjectCrypto crypto = rabbit.crypto(collection);

      String name = rabbit.getString("name");
      String magicWord = crypto.getString("magicWord");

      System.out.println(new Rabbit(name, magicWord));
    }
  }

}
