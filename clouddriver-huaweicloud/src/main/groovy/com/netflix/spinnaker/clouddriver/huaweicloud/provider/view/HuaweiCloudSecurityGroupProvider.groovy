package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.frigga.Names
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.SecurityGroupProvider
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SECURITY_GROUPS

@Slf4j
@Component
class HuaweiCloudSecurityGroupProvider implements SecurityGroupProvider<HuaweiCloudSecurityGroup> {
  final String cloudProvider = HuaweiCloudProvider.ID
  final Cache cacheView
  final ObjectMapper objectMapper
  private final HuaweiCloudInfrastructureProvider huaweicloudProvider

  @Autowired
  HuaweiCloudSecurityGroupProvider(HuaweiCloudInfrastructureProvider tCloudProvider, Cache cacheView, ObjectMapper objectMapper) {
    this.huaweicloudProvider = tCloudProvider
    this.cacheView = cacheView
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAll(boolean includeRules) {
    log.info("Enter HuaweiCloudSecurityGroupProvider getAll,includeRules=${includeRules}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', '*'), includeRules)
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByRegion(boolean includeRules, String region) {
    log.info("Enter HuaweiCloudSecurityGroupProvider getAllByRegion,includeRules=${includeRules},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', '*', region), includeRules)
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccount(boolean includeRules, String account) {
    log.info("Enter HuaweiCloudSecurityGroupProvider getAllByAccount,includeRules=${includeRules},account=${account}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, '*'), includeRules)
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccountAndName(boolean includeRules, String account, String securityGroupName) {
    log.info("Enter HuaweiCloudSecurityGroupProvider getAllByAccountAndName,includeRules=${includeRules}," +
      "account=${account},securityGroupName=${securityGroupName}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account,'*'), includeRules)
  }

  @Override
  Set<HuaweiCloudSecurityGroup> getAllByAccountAndRegion(boolean includeRules, String account, String region) {
    log.info("Enter HuaweiCloudSecurityGroupProvider getAllByAccountAndRegion,includeRules=${includeRules}," +
      "account=${account},region=${region}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*','*', account, region), includeRules)
  }

  @Override
  HuaweiCloudSecurityGroup get(String account, String region, String securityGroupName, String other) {
    log.info("Enter HuaweiCloudSecurityGroupProvider get,account=${account},region=${region},securityGroupName=${securityGroupName}")
    getAllMatchingKeyPattern(Keys.getSecurityGroupKey('*', securityGroupName, account, region), true)[0]
  }

  @Override
  HuaweiCloudSecurityGroup getById(String account, String region, String id, String vpcId) {
    //TO-DO
    log.info("Enter HuaweiCloudSecurityGroupProvider get,account=${account},region=${region},id=${id}")
    return get(account,region,id,vpcId)
  }

  Set<HuaweiCloudSecurityGroup> getAllMatchingKeyPattern(String pattern, boolean includeRules) {
    log.info("Enter getAllMatchingKeyPattern pattern = ${pattern}")
    loadResults(includeRules, cacheView.filterIdentifiers(SECURITY_GROUPS.ns, pattern))
  }

  Set<HuaweiCloudSecurityGroup> loadResults(boolean includeRules, Collection<String> identifiers) {
    def transform = this.&fromCacheData.curry(includeRules)
    def data = cacheView.getAll(SECURITY_GROUPS.ns, identifiers, RelationshipCacheFilter.none())
    def transformed = data.collect(transform)

    return transformed
  }

  HuaweiCloudSecurityGroup fromCacheData(boolean includeRules, CacheData cacheData) {
    //log.info("securityGroup cacheData = ${cacheData.id},${cacheData.attributes[SECURITY_GROUPS.ns]}")
    HuaweiCloudSecurityGroupDescription sg = objectMapper.convertValue(cacheData.attributes[SECURITY_GROUPS.ns], HuaweiCloudSecurityGroupDescription)
    def parts = Keys.parse(cacheData.id)
    def names = Names.parseName(sg.securityGroupName)

    new HuaweiCloudSecurityGroup(
      id: sg.securityGroupId,
      name: sg.securityGroupName,
      description: sg.securityGroupDesc,
      accountName: parts.account?: "none",
      application: names?.app?: "none",
      region: parts.region?: "none",
      inboundRules: [],
      outboundRules: [],
      inRules: sg.inRules,
      outRules: sg.outRules
    )
  }

}
