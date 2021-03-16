package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent


import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstanceHealth
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.*

@Slf4j
@InheritConstructors
class HuaweiCloudInstanceCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(INSTANCES.ns),
    INFORMATIVE.forType(SERVER_GROUPS.ns),
    INFORMATIVE.forType(CLUSTERS.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load instance data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }

    HuaweiElasticCloudServerClient ecsClient = new HuaweiElasticCloudServerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def result = ecsClient.getInstances()
    result.each {
      def tags = ecsClient.getInstanceTags(it.getId())
      def serverGroupName = tags.find {
        it.getKey() == HuaweiAutoScalingClient.defaultServerGroupTagKey
      }?.getValue()

      // security groups
      def sgIds = it.getSecurityGroups().collect{
        it.getId()
      }

      // vpc
      def vpcId = ""
      def privateIps = []
      def publicIps = []
      def vpcSet = it.getAddresses().keySet() as String[]
      if (vpcSet.size() > 0) {
        vpcId = vpcSet[0]
        // address
        def addresses = it.getAddresses()[vpcId]
        def privateIp = addresses.find {
          it.getOsEXTIPSType().toString() == "fixed"
        }?.getAddr()
        if (privateIp) {
          privateIps.add(privateIp)
        }
        def publicIp = addresses.find {
          it.getOsEXTIPSType().toString() == "floating"
        }?.getAddr()
        if (publicIp) {
          publicIps.add(publicIp)
        }
      }

      def launchTime = 0
      if (it.getOsSRVUSGLaunchedAt()) {
        launchTime = HuaweiElasticCloudServerClient.ConvertIsoDateTime it.getOsSRVUSGLaunchedAt() + "Z"
      }
      def huaweicloudInstance = new HuaweiCloudInstance(
        account: accountName,
        name: it.getId(),
        instanceName: it.getName(),
        launchTime: launchTime ? launchTime.time : 0,
        //launchedTime: it.getOsSRVUSGLaunchedAt(),
        launchedTime: launchTime,
        zone: it.getOsEXTAZAvailabilityZone(),
        vpcId: vpcId,
        //subnetId: "",
        privateIpAddresses: privateIps,
        publicIpAddresses: publicIps,
        imageId: it.getImage(),
        instanceType: it.getFlavor(),
        securityGroupIds: sgIds,
        instanceHealth: new HuaweiCloudInstanceHealth(instanceStatus: it.getStatus()),
        serverGroupName: serverGroupName
      )

      if (tags) {
        tags.each { tag->
          huaweicloudInstance.tags.add(["key": tag.getKey(), "value": tag.getValue()])
        }
      }

      def instances = namespaceCache[INSTANCES.ns]
      def instanceKey = Keys.getInstanceKey it.getId(), this.accountName, this.region

      instances[instanceKey].attributes.instance = huaweicloudInstance

      def moniker = huaweicloudInstance.moniker
      if (moniker) {
        def clusterKey = Keys.getClusterKey moniker.cluster, moniker.app, accountName
        def serverGroupKey = Keys.getServerGroupKey huaweicloudInstance.serverGroupName, accountName, region
        instances[instanceKey].relationships[CLUSTERS.ns].add clusterKey
        instances[instanceKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      }
      null
    }
    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults)
    log.info 'finish loads instance data.'
    log.info "Caching ${namespaceCache[INSTANCES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}
