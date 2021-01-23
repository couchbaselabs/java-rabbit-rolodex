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
import com.fasterxml.jackson.databind.JsonNode;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static com.couchbase.client.java.ClusterOptions.clusterOptions;
import static com.couchbase.client.java.diagnostics.WaitUntilReadyOptions.waitUntilReadyOptions;
import static java.util.Collections.singletonMap;

public class App {
  // Couchbase connection parameters
  private static final String COUCHBASE_CONNECTION_STRING = "localhost";
  private static final String COUCHBASE_USERNAME = "Administrator";
  private static final String COUCHBASE_PASSWORD = "password";
  private static final String COUCHBASE_BUCKET = "default";

  // This example uses a single encryption key with this name
  private static final String ENCRYPTION_KEY_NAME = "myKey";

  public static void main(String[] args) {

    // Configure a CryptoManager
    Keyring keyring = keyringWithRandomKey();
    CryptoManager cryptoManager = simpleCryptoManager(keyring);

    // Connect to Couchbase using a custom ClusterEnvironment
    ClusterEnvironment env = ClusterEnvironment.builder()
        .cryptoManager(cryptoManager)
        .build();
    Cluster cluster = Cluster.connect(COUCHBASE_CONNECTION_STRING,
        clusterOptions(COUCHBASE_USERNAME, COUCHBASE_PASSWORD)
            .environment(env));

    // Open the bucket and grab a reference to the default collection
    Bucket bucket = cluster.bucket(COUCHBASE_BUCKET);
    bucket.waitUntilReady(Duration.ofSeconds(10), waitUntilReadyOptions()
        .serviceTypes(ServiceType.KV));
    Collection collection = bucket.defaultCollection();

    // Look out, hare they come!
    List<Rabbit> rolodex = Arrays.asList(
        new Rabbit("Nibbles", "Alakazam"),
        new Rabbit("Floppy", "Ajji Majji la Tarajji"),
        new Rabbit("Booper", "Xyzzy"));

    // Write the list to Couchbase. The "magicWord" fields are
    // encrypted by the client when each Rabbit is converted to JSON.
    collection.upsert("rolodex", rolodex);

    // Verify the fields were encrypted by reading the document back
    // as a Jackson JsonNode. This doesn't decrypt anything,
    // since JsonNode doesn't have any @Encrypted annotations.
    JsonNode encrypted = collection.get("rolodex")
        .contentAs(JsonNode.class);

    System.out.println();
    System.out.println("Document as stored in database:");
    System.out.println(encrypted.toPrettyString());

    // Read the document again, but this time we'll
    // decrypt the fields.
    //
    // If we were loading a single Rabbit, we could use
    // "contentAs(Rabbit.class)" -- but since we're loading
    // a List of rabbits, we use a TypeRef. The TypeRef is
    // converted to a Jackson TypeReference behind the scenes.
    List<Rabbit> decrypted = collection.get("rolodex")
        .contentAs(new TypeRef<List<Rabbit>>() {
        });

    System.out.println();
    System.out.println("Decrypted rolodex");
    System.out.println("=================");
    decrypted.forEach(System.out::println);
    System.out.println();

    cluster.disconnect();
    env.shutdown();
  }

  /**
   * Returns a keyring holding a single randomly generated key
   * named {@value #ENCRYPTION_KEY_NAME}.
   * <p>
   * Because this key isn't stored anywhere, data encrypted with
   * the key cannot be decrypted after the program ends.
   * <p>
   * A real application would load the key from a secure location,
   * and use same same key for each run.
   *
   * @see com.couchbase.client.encryption.KeyStoreKeyring
   * @see com.couchbase.client.encryption.FilesystemKeyring
   * @see com.couchbase.client.encryption.EnvironmentVariableKeyring
   */
  private static Keyring keyringWithRandomKey() {
    byte[] keyBytes = new byte[64];
    new SecureRandom().nextBytes(keyBytes);
    return Keyring.fromMap(singletonMap(ENCRYPTION_KEY_NAME, keyBytes));
  }

  /**
   * Returns a crypto manager configured to use the standard
   * AEAD_AES_256_CBC_HMAC_SHA512 encryption algorithm with
   * a single (default) encrypter that uses the key named
   * {@value #ENCRYPTION_KEY_NAME}.
   */
  private static CryptoManager simpleCryptoManager(Keyring keyring) {
    AeadAes256CbcHmacSha512Provider aes = AeadAes256CbcHmacSha512Provider.builder()
        .keyring(keyring)
        .build();

    return DefaultCryptoManager.builder()
        .decrypter(aes.decrypter())
        .defaultEncrypter(aes.encrypterForKey(ENCRYPTION_KEY_NAME))
        .build();
  }
}
