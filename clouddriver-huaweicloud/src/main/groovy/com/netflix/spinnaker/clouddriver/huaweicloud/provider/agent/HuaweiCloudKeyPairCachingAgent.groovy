package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudKeyPair
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.KEY_PAIRS

@Slf4j
@InheritConstructors
class HuaweiCloudKeyPairCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(KEY_PAIRS.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load key pair data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault {
        id -> new MutableCacheData(id as String)
      }
    }

    HuaweiElasticCloudServerClient ecsClient = new HuaweiElasticCloudServerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region)

    def result = ecsClient.getKeyPairs()
    result.each {
      def keyPair = it.getKeypair()
      def huaweicloudKeyPair = new HuaweiCloudKeyPair(
        keyId: keyPair.getName(),
        keyName: keyPair.getName(),
        keyFingerprint: keyPair.getFingerprint(),
        region: this.region,
        account: this.accountName
      )

      def keyPairs = namespaceCache[KEY_PAIRS.ns]
      def keyPairKey = Keys.getKeyPairKey huaweicloudKeyPair.keyName, this.accountName, this.region
      keyPairs[keyPairKey].attributes.keyPair = huaweicloudKeyPair
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads key pair data.'
    log.info "Caching ${namespaceCache[KEY_PAIRS.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
