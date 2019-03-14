/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.integration;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.ksql.KsqlContext;
import io.confluent.ksql.KsqlContextTestUtil;
import io.confluent.ksql.function.InternalFunctionRegistry;
import io.confluent.ksql.function.MutableFunctionRegistry;
import io.confluent.ksql.function.UdfLoaderUtil;
import io.confluent.ksql.metastore.MetaStore;
import io.confluent.ksql.query.QueryId;
import io.confluent.ksql.services.ServiceContext;
import io.confluent.ksql.util.KsqlConfig;
import io.confluent.ksql.util.PersistentQueryMetadata;
import io.confluent.ksql.util.QueryMetadata;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.rules.ExternalResource;

public final class TestKsqlContext extends ExternalResource {

  private final IntegrationTestHarness testHarness;
  private final Map<String, Object> additionalConfig;
  private final MutableFunctionRegistry functionRegistry;
  private KsqlContext delegate;

  TestKsqlContext(
      final IntegrationTestHarness testHarness,
      final Map<String, Object> additionalConfig
  ) {
    this.testHarness = Objects.requireNonNull(testHarness, "testHarness");
    this.additionalConfig = Objects.requireNonNull(additionalConfig, "additionalConfig");
    this.functionRegistry = new InternalFunctionRegistry();

    UdfLoaderUtil.load(functionRegistry);
  }

  public ServiceContext getServiceContext() {
    return delegate.getServiceContext();
  }

  public MetaStore getMetaStore() {
    return delegate.getMetaStore();
  }

  public List<QueryMetadata> sql(final String sql) {
    return delegate.sql(sql);
  }

  List<PersistentQueryMetadata> getPersistentQueries() {
    return delegate.getPersistentQueries();
  }

  void terminateQuery(final QueryId queryId) {
    delegate.terminateQuery(queryId);
  }

  @Override
  protected void before() {
    final KsqlConfig ksqlConfig = KsqlContextTestUtil.createKsqlConfig(
        testHarness.kafkaBootstrapServers(),
        additionalConfig
    );

    final SchemaRegistryClient srClient = testHarness
        .getServiceContext()
        .getSchemaRegistryClient();

    delegate = KsqlContextTestUtil.create(ksqlConfig, srClient, functionRegistry);
  }

  @Override
  protected void after() {
    delegate.close();
  }
}