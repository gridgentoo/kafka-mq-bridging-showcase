/*
 * Copyright (C) open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package de.huk.hsa.showcase.kafka.consumer.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Properties;

/**
 * Custom health check which signals that application is 'ready'.
 */
@Readiness
@ApplicationScoped
public class ReadinessHealthCheck implements HealthCheck {

  private static final Logger LOG = LoggerFactory.getLogger(ReadinessHealthCheck.class);

  @Inject
  @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
  private String kafkaServer;

  @Inject
  @ConfigProperty(name = "mp.messaging.incoming.message.group.id")
  private String groupId;

  @Override
  public HealthCheckResponse call() {
    boolean up = isReady();
    return HealthCheckResponse
        .named(this
            .getClass()
            .getSimpleName())
        .state(up)
        .build();
  }

  private boolean isReady() {
    AdminClient adminClient = createAdminClient();
    return checkIfBarConsumerGroupRegistered(adminClient);
  }

  private AdminClient createAdminClient() {
    Properties connectionProperties = new Properties();
    connectionProperties.put("bootstrap.servers", kafkaServer);
    AdminClient adminClient = AdminClient.create(connectionProperties);
    return adminClient;
  }

  private boolean checkIfBarConsumerGroupRegistered(final AdminClient adminClient) {
    KafkaFuture<Collection<ConsumerGroupListing>> consumerGroupsFuture = adminClient
        .listConsumerGroups()
        .valid();

    try {
      Collection<ConsumerGroupListing> consumerGroups = consumerGroupsFuture.get();
      for (ConsumerGroupListing consumerGroup : consumerGroups) {
        LOG.info("groupId: " + consumerGroup.groupId());
      }
      return consumerGroups
          .stream()
          .anyMatch(group -> group
              .groupId()
              .equals(groupId));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }
}
