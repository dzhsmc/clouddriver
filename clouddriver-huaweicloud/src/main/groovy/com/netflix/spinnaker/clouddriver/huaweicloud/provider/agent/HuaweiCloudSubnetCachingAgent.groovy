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
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSubnetDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SUBNETS

@Slf4j
class HuaweiCloudSubnetCachingAgent implements CachingAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HuaweiCloudNamedAccountCredentials credentials
  final String providerName = HuaweiCloudInfrastructureProvider.name

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(SUBNETS.ns)
  ] as Set


  HuaweiCloudSubnetCachingAgent(
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

    def subnets = loadSubnetsAll()

    List<CacheData> data = subnets.collect() { HuaweiCloudSubnetDescription subnet ->
      Map<String, Object> attributes = [(SUBNETS.ns): subnet]
      new DefaultCacheData(Keys.getSubnetKey(subnet.subnetId, accountName, region),
        attributes,  [:])
    }

    log.info("Caching ${data.size()} items in ${agentType}")
    new DefaultCacheResult([(SUBNETS.ns): data])
  }

  private Set<HuaweiCloudSubnetDescription> loadSubnetsAll() {
    HuaweiVirtualPrivateCloudClient vpcClient = new HuaweiVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def subnetSet = vpcClient.getSubnetsAll()

    def subnetDescriptionSet =  subnetSet.collect {
      def subnetDesc = new HuaweiCloudSubnetDescription()
      subnetDesc.networkId = it.getId()
      subnetDesc.vpcId = it.getVpcId()
      subnetDesc.subnetId = it.getNeutronSubnetId()
      subnetDesc.subnetName = it.getName()
      subnetDesc.cidrBlock = it.getCidr()
      subnetDesc
    }
    return subnetDescriptionSet
  }

}
