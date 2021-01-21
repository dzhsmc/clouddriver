package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials

abstract class AbstractHuaweiCloudCachingAgent implements CachingAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HuaweiCloudNamedAccountCredentials credentials
  final String providerName = HuaweiCloudInfrastructureProvider.name

  final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  AbstractHuaweiCloudCachingAgent(HuaweiCloudNamedAccountCredentials credentials,
                              ObjectMapper objectMapper,
                              String region) {
    this.credentials = credentials
    this.objectMapper = objectMapper
    this.region = region
    this.accountName = credentials.name
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }
}
