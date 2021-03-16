package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudNetworkDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import groovy.util.logging.Slf4j
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.NETWORKS

@Slf4j
class HuaweiCloudNetworkCachingAgent implements CachingAgent, AccountAware{
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HuaweiCloudNamedAccountCredentials credentials
  final String providerName = HuaweiCloudInfrastructureProvider.name

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(NETWORKS.ns)
  ] as Set


  HuaweiCloudNetworkCachingAgent(
    HuaweiCloudNamedAccountCredentials creds,
    ObjectMapper objectMapper,
    String region
  ) {
    this.accountName = creds.name
    this.credentials = creds
    this.objectMapper = objectMapper
    this.region = region
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Describing items in ${agentType}")

    def networks = loadNetworksAll()

    List<CacheData> data = networks.collect() { HuaweiCloudNetworkDescription network ->
      Map<String, Object> attributes = [(NETWORKS.ns): network]
      new DefaultCacheData(Keys.getNetworkKey(network.vpcId, accountName, region),
        attributes,  [:])
    }

    log.info("Caching ${data.size()} items in ${agentType}")
    new DefaultCacheResult([(NETWORKS.ns): data])
  }

  private Set<HuaweiCloudNetworkDescription> loadNetworksAll() {
    HuaweiVirtualPrivateCloudClient vpcClient = new HuaweiVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def networkSet = vpcClient.getNetworksAll()  //vpc

    def networkDescriptionSet =  networkSet.collect {
      def networkDesc = new HuaweiCloudNetworkDescription()
      networkDesc.vpcId = it.getId()
      networkDesc.vpcName = it.getName()
      networkDesc.cidrBlock = it.getCidr()
      networkDesc
    }
    return networkDescriptionSet
  }

}
