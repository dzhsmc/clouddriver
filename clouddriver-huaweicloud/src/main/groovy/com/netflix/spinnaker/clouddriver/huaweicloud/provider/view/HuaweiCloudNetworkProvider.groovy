package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.NetworkProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudNetwork
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudNetworkDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.NETWORKS


@Slf4j
@RestController
@Component
class HuaweiCloudNetworkProvider implements NetworkProvider<HuaweiCloudNetwork> {
  final String cloudProvider = HuaweiCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final HuaweiCloudInfrastructureProvider huaweicloudProvider

  @Autowired
  HuaweiCloudNetworkProvider(HuaweiCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.huaweicloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudNetwork> getAll() {
    getAllMatchingKeyPattern(Keys.getNetworkKey('*', '*', '*'))
  }

  Set<HuaweiCloudNetwork> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(NETWORKS.ns, pattern))
  }

  Set<HuaweiCloudNetwork> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(NETWORKS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  HuaweiCloudNetwork fromCacheData(CacheData cacheData) {
    HuaweiCloudNetworkDescription vnet = objectMapper.convertValue(cacheData.attributes[NETWORKS.ns], HuaweiCloudNetworkDescription)
    def parts = Keys.parse(cacheData.id)
    //log.info("HuaweiCloudNetworkDescription id = ${cacheData.id}, parts = ${parts}")

    new HuaweiCloudNetwork(
      id: vnet.vpcId,
      name: vnet.vpcName,
      cidrBlock: vnet.cidrBlock,
      account: parts.account?: "none",
      region: parts.region?: "none",
    )
  }
}
