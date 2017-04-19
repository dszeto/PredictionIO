/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.predictionio.data.storage.elasticsearch

import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.predictionio.data.storage.BaseStorageClient
import org.apache.predictionio.data.storage.StorageClientConfig
import org.apache.predictionio.data.storage.StorageClientException
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

import grizzled.slf4j.Logging

case class ESClient(
    hosts: Seq[HttpHost],
    basicAuth: Option[(String, String)] = None) {

  def open(): RestClient = {
    try {
      var builder = RestClient.builder(hosts: _*)
      builder = basicAuth match {
        case Some((username, password)) => builder.setHttpClientConfigCallback(
          new BasicAuthProvider(username, password))
        case None                       => builder}
      builder.build()
    } catch {
      case e: Throwable =>
        throw new StorageClientException(e.getMessage, e)
    }
  }
}

class StorageClient(val config: StorageClientConfig)
  extends BaseStorageClient with Logging {

  override val prefix = "ES"

  val usernamePassword = (
    config.properties.get("USERNAME"),
    config.properties.get("PASSWORD"))
  val optionalBasicAuth: Option[(String, String)] = usernamePassword match {
    case (Some(username), Some(password)) => Some(username, password)
    case (Some(username), None)           => Some(username, "")
    case (None, Some(password))           => Some("", password)
    case (None, None)                     => None}

  val client = ESClient(ESUtils.getHttpHosts(config), optionalBasicAuth)
}

class BasicAuthProvider(
    val username: String,
    val password: String)
  extends HttpClientConfigCallback {

  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(
    AuthScope.ANY,
    new UsernamePasswordCredentials(username, password))

  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder
  ): HttpAsyncClientBuilder = {
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
  }
}
