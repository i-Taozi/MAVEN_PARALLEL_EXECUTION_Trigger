/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.ssl;

import com.mapr.web.security.SslConfig;
import com.mapr.web.security.WebSecurityManager;

/**
 * Provides SSL credentials for Server using {@link WebSecurityManager}.
 */
class SSLCredentialsProviderMaprServer extends SSLCredentialsProvider {

  private final SslConfig sslConfig = WebSecurityManager.getSslConfig();

  @Override
  String getTrustStoreType(String propertyName, String defaultValue) {
    return sslConfig.getServerTruststoreType();
  }

  @Override
  String getTrustStoreLocation(String propertyName, String defaultValue) {
    return sslConfig.getServerTruststoreLocation();
  }

  @Override
  String getTrustStorePassword(String propertyName, String defaultValue) {
    return String.valueOf(sslConfig.getServerTruststorePassword());
  }

  @Override
  String getKeyStoreType(String propertyName, String defaultValue) {
    return sslConfig.getServerKeystoreType();
  }

  @Override
  String getKeyStoreLocation(String propertyName, String defaultValue) {
    return sslConfig.getServerKeystoreLocation();
  }

  @Override
  String getKeyStorePassword(String propertyName, String defaultValue) {
    return String.valueOf(sslConfig.getServerKeystorePassword());
  }

  @Override
  String getKeyPassword(String propertyName, String defaultValue) {
    return String.valueOf(sslConfig.getServerKeyPassword());
  }
}
