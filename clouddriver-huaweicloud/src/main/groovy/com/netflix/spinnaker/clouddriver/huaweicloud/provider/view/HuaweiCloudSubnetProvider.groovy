package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SubnetProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSubnet
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSubnetDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SUBNETS



@Slf4j
@Component
class HuaweiCloudSubnetProvider implements SubnetProvider<HuaweiCloudSubnet> {
  final String cloudProvider = HuaweiCloudProvider.ID
  private final Cache cacheView
  final ObjectMapper objectMapper
  private final HuaweiCloudInfrastructureProvider huaweicloudProvider

  @Autowired
  HuaweiCloudSubnetProvider(HuaweiCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.huaweicloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudSubnet> getAll() {
    getAllMatchingKeyPattern(Keys.getSubnetKey('*', '*', '*'))
  }

  Set<HuaweiCloudSubnet> getAllMatchingKeyPattern(String pattern) {
    loadResults(cacheView.filterIdentifiers(SUBNETS.ns, pattern))
  }

  Set<HuaweiCloudSubnet> loadResults(Collection<String> identifiers) {
    def data = cacheView.getAll(SUBNETS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(this.&fromCacheData)

    return transformed
  }

  HuaweiCloudSubnet fromCacheData(CacheData cacheData) {
    HuaweiCloudSubnetDescription subnet = objectMapper.convertValue(cacheData.attributes[SUBNETS.ns], HuaweiCloudSubnetDescription)
    def parts = Keys.parse(cacheData.id)

    new HuaweiCloudSubnet(
      id: subnet.networkId,
      name: subnet.subnetName,
      vpcId: subnet.vpcId,
      cidrBlock: subnet.cidrBlock,
      purpose: subnet.subnetId,
      account: parts.account?: "unknown",
      region: parts.region?: "unknown"
    )
  }
}
