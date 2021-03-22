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
import com.netflix.spinnaker.clouddriver.core.provider.agent.HealthProvidingCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiLoadBalancerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTargetHealth
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.HEALTH_CHECKS

@Slf4j
class HuaweiCloudLoadBalancerInstanceStateCachingAgent implements CachingAgent, HealthProvidingCachingAgent, AccountAware{
  final String providerName = HuaweiCloudInfrastructureProvider.name
  HuaweiCloudNamedAccountCredentials credentials
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final static String healthId = "huaweicloud-load-balancer-instance-health"

  HuaweiCloudLoadBalancerInstanceStateCachingAgent(HuaweiCloudNamedAccountCredentials credentials,
                                               ObjectMapper objectMapper,
                                               String region) {
    this.credentials = credentials
    this.accountName = credentials.name
    this.region = region
    this.objectMapper = objectMapper
  }


  @Override
  String getHealthId() {
    healthId
  }

  @Override
  String getProviderName() {
    providerName
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  String getAccountName() {
    accountName
  }

  @Override
  Collection<AgentDataType> getProvidedDataTypes() {
    types
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter loadData in ${agentType}")

    def targetHealths = getLoadBalancerTargetHealth()
    Collection<String> evictions = providerCache.filterIdentifiers(HEALTH_CHECKS.ns, Keys.getTargetHealthKey('*', '*',
      '*', '*', accountName, region))

    List<CacheData> data = targetHealths.collect() { HuaweiCloudLoadBalancerTargetHealth targetHealth ->
      Map<String, Object> attributes = ["targetHealth": targetHealth]
      def targetHealthKey = Keys.getTargetHealthKey(targetHealth.loadBalancerId, targetHealth.listenerId,
                                               targetHealth.poolId, targetHealth.instanceId, accountName, region)
      def keepKey = evictions.find {
        it.equals(targetHealthKey)
      }
      if (keepKey) {
        evictions.remove(keepKey)
      }
      new DefaultCacheData(targetHealthKey, attributes, [:])
    }

    log.info("Caching ${data.size()} items evictions ${evictions.size()} items in ${agentType}")
    new DefaultCacheResult([(HEALTH_CHECKS.ns): data], [(HEALTH_CHECKS.ns): evictions])
  }


  private List<HuaweiCloudLoadBalancerTargetHealth> getLoadBalancerTargetHealth() {
    HuaweiLoadBalancerClient client = new HuaweiLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def poolSet = client.getAllPools()
    def memberHealths = client.getAllMembers()
    List<HuaweiCloudLoadBalancerTargetHealth> huaweicloudLBTargetHealths = []
    for (pool in poolSet) {
      def loadBalancerIds = pool.getLoadbalancers()
      def loadBalancerId = ""
      if (loadBalancerIds.size() > 0) {
        loadBalancerId = loadBalancerIds[0].getId()
      }
      def listenerIds = pool.getListeners()
      def listenerId = ""
      if (listenerIds.size() > 0) {
        listenerId = listenerIds[0].getId()
      }

      def poolId = pool.getId()
      for (memberHealth in memberHealths) {
        if (memberHealth.getPoolId() != poolId) {
          continue
        }
        def healthStatus = memberHealth.getOperatingStatus().equals("ONLINE") ? true : false
        def port = memberHealth.getProtocolPort()

        def instanceId = memberHealth.getDeviceId()
        if (!instanceId) {
          def memberId = memberHealth.getId()
          log.warn("The corresponding instance of member ${memberId} is not found")
          continue
        }
        def health = new HuaweiCloudLoadBalancerTargetHealth(instanceId:instanceId,
                    loadBalancerId:loadBalancerId, listenerId:listenerId, poolId:poolId,
                    healthStatus:healthStatus, port:port )
        huaweicloudLBTargetHealths.add(health)
      }
    }

    return huaweicloudLBTargetHealths
  }
}
