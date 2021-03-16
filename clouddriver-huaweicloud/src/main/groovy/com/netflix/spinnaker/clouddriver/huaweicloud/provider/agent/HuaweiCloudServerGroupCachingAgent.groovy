package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.*
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiLoadBalancerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudServerGroup
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudInstanceProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.moniker.Moniker
import com.netflix.spinnaker.moniker.Namer
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.*

@Slf4j
class HuaweiCloudServerGroupCachingAgent extends AbstractHuaweiCloudCachingAgent implements OnDemandAgent {
  String onDemandAgentType = "$agentType-OnDemand"

  final OnDemandMetricsSupport metricsSupport
  final Namer<HuaweiCloudBasicResource> namer

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(APPLICATIONS.ns),
    AUTHORITATIVE.forType(SERVER_GROUPS.ns),
    INFORMATIVE.forType(CLUSTERS.ns),
    INFORMATIVE.forType(INSTANCES.ns),
    INFORMATIVE.forType(LOAD_BALANCERS.ns)  // todo
  ] as Set

  HuaweiCloudServerGroupCachingAgent(
    HuaweiCloudNamedAccountCredentials credentials,
    ObjectMapper objectMapper,
    Registry registry,
    String region
  ) {
    super(credentials, objectMapper, region)
    this.metricsSupport = new OnDemandMetricsSupport(
      registry,
      this,
      "$HuaweiCloudProvider.ID:$OnDemandType.ServerGroup")
    this.namer = NamerRegistry.lookup()
      .withProvider(HuaweiCloudProvider.ID)
      .withAccount(credentials.name)
      .withResource(HuaweiCloudBasicResource)
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    Long start = System.currentTimeMillis()
    log.info "start load data"
    HuaweiAutoScalingClient client = new HuaweiAutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def serverGroups = loadAsgAsServerGroup(client)
    def toEvictOnDemandCacheData = [] as List<CacheData>
    def toKeepOnDemandCacheData = [] as List<CacheData>

    def serverGroupKeys = serverGroups.collect {
      Keys.getServerGroupKey(it.name, credentials.name, region)
    } as Set<String>

    def pendingOnDemandRequestKeys = providerCache
      .filterIdentifiers(
      ON_DEMAND.ns,
      Keys.getServerGroupKey("*", "*", credentials.name, region))
      .findAll { serverGroupKeys.contains(it) }

    def pendingOnDemandRequestsForServerGroups = providerCache.getAll(ON_DEMAND.ns, pendingOnDemandRequestKeys)
    pendingOnDemandRequestsForServerGroups.each {
      if (it.attributes.cacheTime < start && it.attributes.processedCount > 0) {
        toEvictOnDemandCacheData << it
      } else {
        toKeepOnDemandCacheData << it
      }
    }

    CacheResult result = buildCacheResult(
      serverGroups, toKeepOnDemandCacheData, toEvictOnDemandCacheData)

    result.cacheResults[ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }
    result
  }

  private CacheResult buildCacheResult(Collection<HuaweiCloudServerGroup> serverGroups,
                                       Collection<CacheData> toKeepOnDemandCacheData,
                                       Collection<CacheData> toEvictOnDemandCacheData) {
    log.info "Start build cache for $agentType"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Collection<String>> evictions = [(ON_DEMAND.ns):toEvictOnDemandCacheData*.id]

    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace->[:].withDefault {id->new MutableCacheData(id as String)}
    }

    serverGroups.each {
      Moniker moniker = namer.deriveMoniker it
      def applicationName = moniker.app
      def clusterName = moniker.cluster

      if (applicationName == null || clusterName == null) {
        return
      }

      def serverGroupKey = Keys.getServerGroupKey it.name, accountName, region
      def clusterKey = Keys.getClusterKey clusterName, applicationName, accountName
      def appKey = Keys.getApplicationKey applicationName
      List<String> instanceKeys = []
      List<String> loadBalancerKeys = []

      Set<HuaweiCloudInstance> instances = it.instances
      instances.each {instance ->
        instanceKeys.add Keys.getInstanceKey(instance.name as String, accountName, region)
      }

      def loadBalancerIds = it.loadBalancers
      loadBalancerIds.each { String lbId ->
        loadBalancerKeys.add Keys.getLoadBalancerKey(lbId, accountName, region)
      }

      // application
      def applications = namespaceCache[APPLICATIONS.ns]
      applications[appKey].attributes.name = applicationName
      applications[appKey].relationships[CLUSTERS.ns].add clusterKey
      applications[appKey].relationships[SERVER_GROUPS.ns].add serverGroupKey

      // cluster
      namespaceCache[CLUSTERS.ns][clusterKey].attributes.name = clusterName
      namespaceCache[CLUSTERS.ns][clusterKey].attributes.accountName = accountName
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[APPLICATIONS.ns].add appKey
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[INSTANCES.ns].addAll instanceKeys
      namespaceCache[CLUSTERS.ns][clusterKey].relationships[LOAD_BALANCERS.ns].addAll loadBalancerKeys

      // loadBalancer
      loadBalancerKeys.each { lbKey ->
        namespaceCache[LOAD_BALANCERS.ns][lbKey].relationships[SERVER_GROUPS.ns].add serverGroupKey
      }

      // server group
      def onDemandServerGroupCache = toKeepOnDemandCacheData.find {
        it.attributes.name == serverGroupKey
      }
      if (onDemandServerGroupCache) {
        mergeOnDemandCache onDemandServerGroupCache, namespaceCache
      } else {

        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.asg = it.asg
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.accountName = accountName
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.name = it.name
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.region = it.region
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.launchConfig = it.launchConfig
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.disabled = it.disabled
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].attributes.scalingPolicies = it.scalingPolicies
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[APPLICATIONS.ns].add appKey
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[CLUSTERS.ns].add clusterKey
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[INSTANCES.ns].addAll instanceKeys
        namespaceCache[SERVER_GROUPS.ns][serverGroupKey].relationships[LOAD_BALANCERS.ns].addAll loadBalancerKeys
      }
    }

    namespaceCache.each {String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }
    cacheResults[ON_DEMAND.ns] = toKeepOnDemandCacheData

    CacheResult result = new DefaultCacheResult(
      cacheResults, evictions
    )

    result
  }

  def mergeOnDemandCache(CacheData onDemandServerGroupCache, Map<String, Map<String, CacheData>>  namespaceCache) {
    Map<String, List<MutableCacheData>> onDemandCache = objectMapper.readValue(
      onDemandServerGroupCache.attributes.cacheResults as String,
      new TypeReference<Map<String, List<MutableCacheData>>>() {})

    onDemandCache.each { String namespace, List<MutableCacheData> cacheDataList ->
      if (namespace != 'onDemand') {
        cacheDataList.each {
          def existingCacheData = namespaceCache[namespace][it.id]
          if (!existingCacheData) {
            namespaceCache[namespace][it.id] = it
          } else {
            existingCacheData.attributes.putAll it.attributes
            it.relationships.each { String relationshipName, Collection<String> relationships ->
              existingCacheData.relationships[relationshipName].addAll relationships
            }
          }
        }
      }
    }
  }

  List<HuaweiCloudServerGroup> loadAsgAsServerGroup(HuaweiAutoScalingClient client, String serverGroupName = null) {
    HuaweiLoadBalancerClient lbClient = new HuaweiLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
    def asgs
    if (serverGroupName) {
      asgs = client.getAutoScalingGroupsByName serverGroupName
    } else {
      asgs = client.getAllAutoScalingGroups()
    }

    def launchConfigurations = client.getLaunchConfigurations()
    def pools = lbClient.getAllPools()

    return asgs.collect {
      def autoScalingGroupId = it.getScalingGroupId()
      def autoScalingGroupName = it.getScalingGroupName()
      def disabled = it.getScalingGroupStatus().toString() != 'INSERVICE'
      HuaweiCloudServerGroup serverGroup = new HuaweiCloudServerGroup().with {
        it.accountName = this.accountName
        it.region = this.region
        it.name = autoScalingGroupName
        it.disabled = disabled
        it
      }

      def forwardLBSet = it.getLbaasListeners().collect {
        def poolId = it.getPoolId()
        def listenerId = ""
        def loadBalancerId = ""
        def pool = pools.find {
          it.getId() == poolId
        }

        if (pool && pool.getListeners() && pool.getListeners().size() > 0) {
          listenerId = pool.getListeners()[0].getId()
        }
        if (pool && pool.getLoadbalancers() && pool.getLoadbalancers().size() > 0) {
          loadBalancerId = pool.getLoadbalancers()[0].getId()
        }

        def targetAttr = [
          port: it.getProtocolPort(),
          weight: it.getWeight()
        ] 
        def lbMap = [
          loadBalancerId: loadBalancerId,
          listenerId: listenerId,
          poolId: poolId,
          targetAttributes: [targetAttr]
        ]
        lbMap
      }
      def subnetIds = it.getNetworks().collect{
        it.getId()
      }

      String launchConfigurationId = it.getScalingConfigurationId()
      serverGroup.asg = [
        autoScalingGroupId: autoScalingGroupId,
        launchConfigurationId: launchConfigurationId,
        autoScalingGroupName: autoScalingGroupName,
        zoneSet: it.getAvailableZones(),
        vpcId: it.getVpcId(),
        subnetIdSet: subnetIds,
        minSize: it.getMinInstanceNumber(),
        maxSize: it.getMaxInstanceNumber(),
        desiredCapacity: it.getDesireInstanceNumber(),
        instanceCount: it.getCurrentInstanceNumber(),
        defaultCooldown: it.getCoolDownTime(),
        createdTime: it.getCreateTime(),
        forwardLoadBalancerSet: forwardLBSet,
        healthAuditMethod: it.getHealthPeriodicAuditMethod().getValue(),
        healthPeriodicTime: it.getHealthPeriodicAuditTime().getValue(),
        healthGracePeriod: it.getHealthPeriodicAuditGracePeriod(),
        terminationPolicySet: [it.getInstanceTerminatePolicy().toString()]
      ]

      def asc = launchConfigurations.find {
        it.getScalingConfigurationId() == launchConfigurationId
      }
      def isc = asc.getInstanceConfig()
      def sgIds = isc.getSecurityGroups().collect{
        it.getId()
      }
      def tags = client.getAutoScalingTags(autoScalingGroupId).collect{
        [key: it.getKey(), value: it.getValue()]
      }

      // public IP
      def publicIP = isc.getPublicIp()
      def internetAccessible = [
        publicIpAssigned: false,
        internetMaxBandwidthOut: 0,
        internetChargeType: "TRAFFIC_POSTPAID_BY_HOUR"
      ]

      if (publicIP) {
        internetAccessible.publicIpAssigned = true
        internetAccessible.internetMaxBandwidthOut = publicIP.getEip().getBandwidth().getSize()
        def chargingMode = publicIP.getEip().getBandwidth().getChargingMode().toString()
        if (chargingMode == "bandwidth") {
          internetAccessible.internetChargeType = "BANDWIDTH_POSTPAID_BY_HOUR"
        }
      }
      // disk
      def disks = isc.getDisk()
      def systemDisk = [
        diskSize: 50,
        diskType: "SAS"
      ]
      def dataDisks = []
      disks.each {
        if (it.getDiskType().toString() == "SYS") {
          systemDisk.diskSize = it.getSize()
          systemDisk.diskType = it.getVolumeType().toString()
        } else if (it.getDiskType().toString() == "DATA") {
          def dataDisk = [
            diskSize: it.getSize(),
            diskType: it.getVolumeType().toString()
          ]
          dataDisks.add(dataDisk)
        }
      }

      serverGroup.launchConfig = [
        launchConfigurationName: asc.getScalingConfigurationName(),
        instanceType: isc.getFlavorRef(),
        imageId: isc.getImageRef(),
        keyName: isc.getKeyName(),
        userData: isc.getUserData(),
        securityGroupIds: sgIds,
        instanceTags: tags,
        internetAccessible: internetAccessible,
        systemDisk: systemDisk,
        dataDisks: dataDisks
      ]

      // serverGroup.scalingPolicies = loadScalingPolicies(client, autoScalingGroupId)
      serverGroup.scalingPolicies = []

      def instances = client.getAutoScalingInstances autoScalingGroupId

      instances.each {
        def instance = new HuaweiCloudInstance()
        instance.name = it.getInstanceId()
        serverGroup.instances.add(instance)
      }
      // Cache scaling instance states here
      serverGroup.asg["instances"] = instances
      serverGroup
    }
  }

  /*
  private List<Map> loadScalingPolicies(HuaweiAutoScalingClient client, String autoScalingGroupId=null) {
    def scalingPolicies = client.getScalingPolicies autoScalingGroupId
    scalingPolicies.collect {
      Map<String, Object> asp = objectMapper.convertValue it, ATTRIBUTES
      return asp
    }
  }
  */

  private List<Map> loadAutoScalingInstances(HuaweiAutoScalingClient client, String autoScalingGroupId=null) {
    def autoScalingInstances = client.getAutoScalingInstances autoScalingGroupId
    autoScalingInstances.collect {
      Map<String, Object> asi = objectMapper.convertValue it, ATTRIBUTES
      asi
    }
  }

  @Override
  OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    if (!data.containsKey("serverGroupName") || data.accountName != accountName || data.region != region) {
      return null
    }
    log.info("Enter huaweicloud server group agent handle " + data.serverGroupName)
    HuaweiAutoScalingClient client = new HuaweiAutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
    HuaweiCloudServerGroup serverGroup = metricsSupport.readData {
      loadAsgAsServerGroup(client, data.serverGroupName as String)[0]
    }

    if (!serverGroup) {
      return null
    }

    def cacheResult = metricsSupport.transformData {
      buildCacheResult([serverGroup], null, null)
    }

    def cacheResultAsJson = objectMapper.writeValueAsString cacheResult.cacheResults
    def serverGroupKey = Keys.getServerGroupKey(
      serverGroup?.moniker?.cluster, data.serverGroupName as String, accountName, region)

    if (cacheResult.cacheResults.values().flatten().empty) {
      // Avoid writing an empty onDemand cache record (instead delete any that may have previously existed).
      providerCache.evictDeletedItems ON_DEMAND.ns, [serverGroupKey]
    } else {
      metricsSupport.onDemandStore {
        def cacheData = new DefaultCacheData(
          serverGroupKey,
          10 * 60,
          [
            cacheTime   : new Date(),
            cacheResults: cacheResultAsJson
          ],
          [:]
        )
        providerCache.putCacheData ON_DEMAND.ns, cacheData
      }
    }

    Map<String, Collection<String>> evictions = serverGroup.asg ? [:] : [
      (SERVER_GROUPS.ns): [serverGroupKey]]

    return new OnDemandResult(
      sourceAgentType: getOnDemandAgentType(),
      cacheResult: cacheResult,
      evictions: evictions
    )
  }

  @Override
  boolean handles(OnDemandType type, String cloudProvider) {
    type == OnDemandType.ServerGroup && cloudProvider == HuaweiCloudProvider.ID
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    def keys = providerCache.filterIdentifiers(
      ON_DEMAND.ns,
      Keys.getServerGroupKey("*", "*", credentials.name, region)
    )
    return fetchPendingOnDemandRequests(providerCache, keys)
  }

  private Collection<Map> fetchPendingOnDemandRequests(
    ProviderCache providerCache, Collection<String> keys) {
    return providerCache.getAll(ON_DEMAND.ns, keys, RelationshipCacheFilter.none()).collect {
      def details = Keys.parse(it.id)

      return [
        id            : it.id,
        details       : details,
        moniker       : convertOnDemandDetails(details),
        cacheTime     : it.attributes.cacheTime,
        cacheExpiry   : it.attributes.cacheExpiry,
        processedCount: it.attributes.processedCount,
        processedTime : it.attributes.processedTime
      ]
    }
  }
}
