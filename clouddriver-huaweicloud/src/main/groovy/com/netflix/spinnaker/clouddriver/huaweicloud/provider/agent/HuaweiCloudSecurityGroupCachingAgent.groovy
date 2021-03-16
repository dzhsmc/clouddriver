package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiVirtualPrivateCloudClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupRule
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys

import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.*

@Slf4j
class HuaweiCloudSecurityGroupCachingAgent implements CachingAgent, OnDemandAgent, AccountAware {
  final ObjectMapper objectMapper
  final String region
  final String accountName
  final HuaweiCloudNamedAccountCredentials credentials
  final String providerName = HuaweiCloudInfrastructureProvider.name
  final Registry registry
  final OnDemandMetricsSupport metricsSupport
  String onDemandAgentType = "${agentType}-OnDemand"

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(SECURITY_GROUPS.ns)
  ] as Set

  HuaweiCloudSecurityGroupCachingAgent(HuaweiCloudNamedAccountCredentials creds,
                                   ObjectMapper objectMapper,
                                   Registry registry,
                                   String region) {
    this.accountName = creds.name
    this.credentials = creds
    this.region = region
    this.objectMapper = objectMapper
    this.registry = registry
    this.metricsSupport = new OnDemandMetricsSupport(registry, this, "${HuaweiCloudProvider.ID}:${OnDemandAgent.OnDemandType.SecurityGroup}")
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  String getProviderName() {
    providerName
  }

  @Override
  String getAccountName() {
    accountName
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == OnDemandAgent.OnDemandType.SecurityGroup && cloudProvider == HuaweiCloudProvider.ID
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.info("Enter HuaweiCloudSecurityGroupCachingAgent handle, params = ${data}")
    if (!data.containsKey("securityGroupId") ||
      !data.containsKey("account") ||
      !data.containsKey("region") ||
      accountName != data.account ||
      region != data.region) {
      log.info("HuaweiCloudSecurityGroupCachingAgent: input params error!")
      return null
    }

    HuaweiCloudSecurityGroupDescription updatedSecurityGroup = null
    HuaweiCloudSecurityGroupDescription evictedSecurityGroup = null
    String securityGroupId = data.securityGroupId as String

    updatedSecurityGroup = metricsSupport.readData {
      loadSecurityGroupById(securityGroupId)
    }
    if (!updatedSecurityGroup) {
      log.info("HuaweiCloudSecurityGroupCachingAgent: Can not find securityGroup ${securityGroupId} in ${region}")
      return null
    }

    def cacheResult = metricsSupport.transformData {
      if (updatedSecurityGroup) {
        return buildCacheResult(providerCache, null, 0, updatedSecurityGroup, null)
      } else {
        evictedSecurityGroup = new HuaweiCloudSecurityGroupDescription(
          securityGroupId: securityGroupId,
          securityGroupName: "unknown",
          securityGroupDesc: "unkonwn",
          lastReadTime: System.currentTimeMillis()
        )
        return buildCacheResult(providerCache, null, 0, null, evictedSecurityGroup)
      }
    }
    Map<String, Collection<String>> evictions = evictedSecurityGroup ? [(SECURITY_GROUPS.ns): [Keys.getSecurityGroupKey(evictedSecurityGroup.securityGroupId, accountName, region)]] : [:]

    log.info("HuaweiCloudSecurityGroupCachingAgent: onDemand cache refresh (data: ${data}, evictions: ${evictions})")
    return new OnDemandAgent.OnDemandResult(
      sourceAgentType: getAgentType(), cacheResult: cacheResult, evictions: evictions
    )
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter HuaweiCloudSecurityGroupCachingAgent loadData in ${agentType}")
    def currentTime = System.currentTimeMillis()
    def securityGroupDescSet = loadSecurityGroupAll()

    log.info("Total SecurityGroup Number = ${securityGroupDescSet.size()} in ${agentType}")
    buildCacheResult(providerCache, securityGroupDescSet, currentTime, null, null)
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    return []
  }

  private Set<HuaweiCloudSecurityGroupDescription> loadSecurityGroupAll() {
    HuaweiVirtualPrivateCloudClient vpcClient = new HuaweiVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def securityGroupSet = vpcClient.getSecurityGroupsAll()

    def securityGroupDescriptionSet =  securityGroupSet.collect {
      def securityGroupDesc = new HuaweiCloudSecurityGroupDescription()
      securityGroupDesc.securityGroupId = it.getId()
      securityGroupDesc.securityGroupName = it.getName()
      securityGroupDesc.securityGroupDesc = it.getDescription()
      def securityGroupRules = it.getSecurityGroupRules()
      def ingressRules = []
      def egressRules = []
      securityGroupRules.each {
        if (it.getDirection().equals("ingress")) {
          ingressRules << it
        }
        if (it.getDirection().equals("egress")) {
          egressRules << it
        }
      }
      securityGroupDesc.inRules = ingressRules.collect { ingress ->
        def inRule = new HuaweiCloudSecurityGroupRule()
        def protocol = ingress.getProtocol()
        if (!protocol) {
          protocol = "ALL"
        }
        def source = ingress.getRemoteIpPrefix()
        if (!source) {
          source = ingress.getRemoteGroupId()
        }
        def portMin = ingress.getPortRangeMin()
        def portMax = ingress.getPortRangeMax()
        def port = ""
        if (!portMin && !portMax) {
          port = "ALL"
        } else if (portMin == portMax) {
          port = portMin
        } else {
          port = portMin + "-" + portMax
        }
        inRule.protocol = protocol
        inRule.port = port
        inRule.cidrBlock = source
        inRule
      }
      securityGroupDesc.outRules = egressRules.collect { egress ->
        def outRule = new HuaweiCloudSecurityGroupRule()
        def protocol = egress.getProtocol()
        if (!protocol) {
          protocol = "ALL"
        }
        def source = egress.getRemoteIpPrefix()
        if (!source) {
          source = egress.getRemoteGroupId()
        }
        def portMin = egress.getPortRangeMin()
        def portMax = egress.getPortRangeMax()
        def port = ""
        if (!portMin && !portMax) {
          port = "ALL"
        } else if (portMin == portMax) {
          port = portMin
        } else {
          port = portMin + "-" + portMax
        }
        outRule.protocol = protocol
        outRule.port = port
        outRule.cidrBlock = source
        outRule
      }
      securityGroupDesc.lastReadTime = System.currentTimeMillis()
      securityGroupDesc
    }
    return securityGroupDescriptionSet
  }

  private HuaweiCloudSecurityGroupDescription loadSecurityGroupById(String securityGroupId) {
    HuaweiVirtualPrivateCloudClient vpcClient = new HuaweiVirtualPrivateCloudClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def securityGroup = vpcClient.getSecurityGroupById(securityGroupId)[0]
    def currentTime = System.currentTimeMillis()
    if (securityGroup) {
      def securityGroupDesc = new HuaweiCloudSecurityGroupDescription(
        securityGroupId: securityGroup.getId(),
        securityGroupName: securityGroup.getName(),
        lastReadTime: currentTime
      )
      def securityGroupRules = it.getSecurityGroupRules()
      def ingressRules = []
      def egressRules = []
      securityGroupRules.each {
        if (it.getDirection().equals("ingress")) {
          ingressRules << it
        }
        if (it.getDirection().equals("egress")) {
          egressRules << it
        }
      }
      securityGroupDesc.inRules = ingressRules.collect { ingress ->
        def inRule = new HuaweiCloudSecurityGroupRule()
        inRule.protocol = ingress.getProtocol()
        inRule.portMin = ingress.getPortRangeMin()
        inRule.portMax = ingress.getPortRangeMax()
        inRule.cidrBlock = ingress.getRemoteIpPrefix()
        inRule
      }
      securityGroupDesc.outRules = egressRules.collect { egress ->
        def outRule = new HuaweiCloudSecurityGroupRule()
        outRule.protocol = egress.getProtocol()
        outRule.portMin = egress.getPortRangeMin()
        outRule.portMax = egress.getPortRangeMax()
        outRule.cidrBlock = egress.getRemoteIpPrefix()
        outRule
      }
      return securityGroupDesc
    }
    return null
  }

  private CacheResult buildCacheResult(ProviderCache providerCache,
                                       Collection<HuaweiCloudSecurityGroupDescription> securityGroups,
                                       long lastReadTime,
                                       HuaweiCloudSecurityGroupDescription updatedSecurityGroup,
                                       HuaweiCloudSecurityGroupDescription evictedSecurityGroup) {
    if (securityGroups) {
      List<CacheData> data = new ArrayList<CacheData>()
      Collection<String> identifiers = providerCache.filterIdentifiers(ON_DEMAND.ns, Keys.getSecurityGroupKey("*","*", accountName, region))
      def onDemandCacheResults = providerCache.getAll(ON_DEMAND.ns, identifiers, RelationshipCacheFilter.none())

      // Add any outdated OnDemand cache entries to the evicted list
      List<String> evictions = new ArrayList<String>()
      Map<String, CacheData> usableOnDemandCacheDatas = [:]
      onDemandCacheResults.each {
        if(it.attributes.cachedTime < lastReadTime){
          evictions.add(it.id)
        } else {
          usableOnDemandCacheDatas[it.id] = it
        }
      }

      securityGroups.each { HuaweiCloudSecurityGroupDescription item ->
        HuaweiCloudSecurityGroupDescription securityGroup = item

        String sgKey = Keys.getSecurityGroupKey(securityGroup.securityGroupId, securityGroup.securityGroupName, accountName, region)

        // Search the current OnDemand update map entries and look for a security group match
        def onDemandSG = usableOnDemandCacheDatas[sgKey]
        if (onDemandSG) {
          if (onDemandSG.attributes.cachedTime > securityGroup.lastReadTime) {
            // Found a security group resource that has been updated since last time was read from Azure cloud
            securityGroup = objectMapper.readValue(onDemandSG.attributes.securityGroup as String, HuaweiCloudSecurityGroupDescription)
          } else {
            // Found a Security Group that has been deleted since last time was read from HuaweiCloud cloud
            securityGroup = null
          }
          // There's no need to keep this entry in the map
          usableOnDemandCacheDatas.remove(sgKey)
        }
        if (securityGroup) {
          data.add(buildCacheData(securityGroup))
        }
      }

      log.info("Caching ${data.size()} items in ${agentType}")

      return new DefaultCacheResult(
        [(SECURITY_GROUPS.ns): data],
        [(ON_DEMAND.ns): evictions])
    }  else {
      if (updatedSecurityGroup) {
        // This is an OnDemand update/edit request for a given security group resource
        // Attempt to add entry into the OnDemand respective cache
        if (updateCache(providerCache, updatedSecurityGroup, "OnDemandUpdated")) {
          CacheData data = buildCacheData(updatedSecurityGroup)
          log.info("Caching 1 OnDemand updated item in ${agentType}")
          return new DefaultCacheResult([(SECURITY_GROUPS.ns): [data]])
        } else {
          return null
        }
      }

      if (evictedSecurityGroup) {
        // This is an OnDemand delete request for a given Azure network security group resource
        // Attempt to add entry into the OnDemand respective cache
        if (updateCache(providerCache, evictedSecurityGroup, "OnDemandEvicted")) {
          log.info("Caching 1 OnDemand evicted item in ${agentType}")
          return new DefaultCacheResult([(SECURITY_GROUPS.ns): []])
        } else {
          return null
        }
      }
    }

    return new DefaultCacheResult([(SECURITY_GROUPS.ns): []])
  }

  private CacheData buildCacheData(HuaweiCloudSecurityGroupDescription securityGroup) {
    Map<String, Object> attributes = [(SECURITY_GROUPS.ns): securityGroup]

    new DefaultCacheData(Keys.getSecurityGroupKey(securityGroup.securityGroupId, securityGroup.securityGroupName, accountName, region), attributes, [:])
  }

  private Boolean updateCache(ProviderCache providerCache, HuaweiCloudSecurityGroupDescription securityGroup, String onDemandCacheType) {
    Boolean foundUpdatedOnDemandSG = false

    if (securityGroup) {
      // Get the current list of all OnDemand requests from the cache
      def cacheResults = providerCache.getAll(ON_DEMAND.ns, [Keys.getSecurityGroupKey(securityGroup.securityGroupId, securityGroup.securityGroupName, accountName, region)])
      if (cacheResults && !cacheResults.isEmpty()) {
        cacheResults.each {
          // cacheResults.each should only return one item which is matching the given security group details
          if (it.attributes.cachedTime > securityGroup.lastReadTime) {
            // Found a newer matching entry in the cache when compared with the current OnDemand request
            foundUpdatedOnDemandSG = true
          }
        }
      }

      if (!foundUpdatedOnDemandSG) {
        // Add entry to the OnDemand respective cache
        def cacheData = new DefaultCacheData(
          Keys.getSecurityGroupKey(securityGroup.securityGroupId, securityGroup.securityGroupName, accountName, region),
          [
            securityGroup: objectMapper.writeValueAsString(securityGroup),
            cachedTime: securityGroup.lastReadTime,
            onDemandCacheType : onDemandCacheType
          ],
          [:]
        )
        providerCache.putCacheData(ON_DEMAND.ns, cacheData)
        return true
      }
    }

    false
  }

}
