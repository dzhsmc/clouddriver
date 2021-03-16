package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstanceType
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCE_TYPES

@Slf4j
@InheritConstructors
class HuaweiCloudInstanceTypeCachingAgent extends AbstractHuaweiCloudCachingAgent {
  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCE_TYPES.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load instance types data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    HuaweiElasticCloudServerClient ecsClient = new HuaweiElasticCloudServerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region,
    )

    def result = ecsClient.getInstanceTypes()
    result.each {
      def extraSpec = it.getOsExtraSpecs()
      def instanceFamily = extraSpec.getEcsPerformancetype()
      def huaweicloudInstanceType = new HuaweiCloudInstanceType(
        name: it.getName(),
        account: this.accountName,
        region: this.region,
        instanceFamily: instanceFamily,
        cpu: it.getVcpus() as int,
        mem: it.getRam()/1024
      )

      def instanceTypes = namespaceCache[INSTANCE_TYPES.ns]
      def instanceTypeKey = Keys.getInstanceTypeKey this.accountName, this.region, huaweicloudInstanceType.name

      instanceTypes[instanceTypeKey].attributes.instanceType = huaweicloudInstanceType
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info "finish loads instance type data."
    log.info "Caching ${namespaceCache[INSTANCE_TYPES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
