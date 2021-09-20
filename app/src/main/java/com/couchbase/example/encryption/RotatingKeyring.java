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

import com.couchbase.client.encryption.Keyring;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Base class for implementing Keyrings that support key rotation.
 */
public abstract class RotatingKeyring implements Keyring {
  protected final String versionDelimiter;

  /**
   * @param versionDelimiter separates a key ID's base name component from the version component.
   */
  protected RotatingKeyring(String versionDelimiter) {
    this.versionDelimiter = requireNonNull(versionDelimiter);
  }

  protected class KeyNameAndVersion {
    private final String name;
    private final String version;

    public KeyNameAndVersion(String name, String version) {
      this.name = requireNonNull(name);
      this.version = requireNonNull(version);
    }

    public String name() {
      return name;
    }

    public String version() {
      return version;
    }

    public String format() {
      return name + versionDelimiter + version;
    }

    @Override
    public String toString() {
      return format();
    }
  }

  public Optional<Key> get(String keyId) {
    KeyNameAndVersion nameAndVersion = parseKeyNameAndVersion(keyId);
    return getKeyBytes(nameAndVersion)
        .map(bytes -> Key.create(nameAndVersion.format(), bytes));
  }

  protected abstract String getPrimaryVersion(String baseName);

  protected abstract Optional<byte[]> getKeyBytes(KeyNameAndVersion keyNameAndVersion);

  protected KeyNameAndVersion parseKeyNameAndVersion(String keyId) {
    int i = keyId.indexOf(versionDelimiter);
    return i == -1
        ? new KeyNameAndVersion(keyId, getPrimaryVersion(keyId))
        : new KeyNameAndVersion(keyId.substring(0, i), keyId.substring(i + versionDelimiter.length()));
  }
}
