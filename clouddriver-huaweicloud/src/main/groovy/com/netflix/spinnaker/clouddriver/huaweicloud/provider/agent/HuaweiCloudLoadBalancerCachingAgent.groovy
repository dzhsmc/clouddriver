package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.AccountAware
import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.DefaultCacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancer
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerCertificate
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerHealthCheck
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerPool
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerRule
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.moniker.Moniker
import com.netflix.spinnaker.moniker.Namer
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiLoadBalancerClient
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import groovy.util.logging.Slf4j
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.APPLICATIONS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.INSTANCES
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.LOAD_BALANCERS
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.ON_DEMAND
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.SERVER_GROUPS


@Slf4j
class HuaweiCloudLoadBalancerCachingAgent implements OnDemandAgent, CachingAgent, AccountAware{
  final String accountName
  final String region
  final ObjectMapper objectMapper
  final String providerName = HuaweiCloudInfrastructureProvider.name
  HuaweiCloudNamedAccountCredentials credentials
  final OnDemandMetricsSupport metricsSupport
  final Namer<HuaweiCloudBasicResource> namer
  String onDemandAgentType = "${agentType}-OnDemand"

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(APPLICATIONS.ns),
    AUTHORITATIVE.forType(LOAD_BALANCERS.ns),
    INFORMATIVE.forType(INSTANCES.ns),
  ] as Set

  private static final TypeReference<Map<String, Object>> ATTRIBUTES = new TypeReference<Map<String, Object>>() {}

  HuaweiCloudLoadBalancerCachingAgent(
    HuaweiCloudNamedAccountCredentials credentials,
    ObjectMapper objectMapper,
    Registry registry,
    String region
  ) {
    this.credentials = credentials
    this.accountName = credentials.name
    this.region = region
    this.objectMapper = objectMapper
    this.metricsSupport = new OnDemandMetricsSupport(
      registry,
      this,
      "${HuaweiCloudProvider.ID}:${OnDemandAgent.OnDemandType.LoadBalancer}")
    this.namer = NamerRegistry.lookup()
      .withProvider(HuaweiCloudProvider.ID)
      .withAccount(credentials.name)
      .withResource(HuaweiCloudBasicResource)
  }

  @Override
  String getAgentType() {
    return "$accountName/$region/${this.class.simpleName}"
  }

  @Override
  String getProviderName() {
    return providerName
  }

  @Override
  String getAccountName() {
    return accountName
  }

  List<HuaweiCloudLoadBalancer> loadLoadBalancerData(String loadBalancerId = null) {
    HuaweiLoadBalancerClient client = new HuaweiLoadBalancerClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def lbSet = []
    def lbIds = []
    if (loadBalancerId) {
      lbSet = client.getLoadBalancerById(loadBalancerId)
    } else {
      lbSet = client.getAllLoadBalancer()
    }
    lbSet.each {
      lbIds.add(it.getId())
    }

    def poolSet = client.getAllPools(lbIds)
    def listenerSet = client.getAllLBListener(lbIds)
    def healthMonitorSet = client.getAllHealthMonitors()
    def membersSet = client.getAllMembers()

    def loadBanancerList =  lbSet.collect {
      HuaweiCloudLoadBalancer loadBalancer = new HuaweiCloudLoadBalancer()
      loadBalancer.region = region
      loadBalancer.accountName = accountName
      loadBalancer.name = it.getName()
      loadBalancer.loadBalancerName = it.getName()
      loadBalancer.id = it.getId()
      loadBalancer.loadBalancerId = it.getId()
      loadBalancer.subnetId = it.getVipSubnetCidrId()
      loadBalancer.vpcId = it.getVpcId()
      loadBalancer.loadBalancerVip = it.getVipAddress()
      loadBalancer.createTime = it.getCreatedAt()

      def queryPools = []
      it.getPools().each {
        def poolId = it.getId()
        def pool = poolSet.find {
          it.getId() == poolId
        }
        if (pool) {
          queryPools.add(pool)
        }
      }
      def refinedPools = queryPools.collect {
        def pool = new HuaweiCloudLoadBalancerPool()
        pool.poolId = it.getId()
        pool.poolName = it.getName()
        pool
      }
      loadBalancer.pools = refinedPools

      def queryListeners = []
      it.getListeners().each {
        def listenerId = it.getId()
        def listener = listenerSet.find {
          it.getId() == listenerId
        }
        if (listener) {
          queryListeners.add(listener)
        }
      }

      def listenerIds = []
      queryListeners.each {
        listenerIds.add(it.getId())
      }
      def policySet = []
      if (listenerIds.size() > 0) {
        policySet = client.getAllL7policies(listenerIds)
      }

      def listeners = queryListeners.collect {
        def listener = new HuaweiCloudLoadBalancerListener()
        listener.listenerId = it.getId()
        def protocol = it.getProtocol()
        if (protocol == "TERMINATED_HTTPS") {
          protocol = "HTTPS"
          listener.certificate = new HuaweiCloudLoadBalancerCertificate()
          listener.certificate.certId = it.getDefaultTlsContainerRef()
          listener.certificate.certCaId = it.getClientCaTlsContainerRef()
        }

        listener.protocol = protocol
        listener.port = it.getProtocolPort()
        listener.listenerName = it.getName()

        if (protocol == "TCP" || protocol == "UDP") {
          def pools = poolSet
          def listenerId = null
          for (int i = 0; i < pools.size(); i++) {
            listenerId = pools[i].getListeners().find {
              it.getId() == listener.listenerId
            }?.getId()
            if (listenerId) {
              def pool = pools[i]
              listener.poolId = pool.getId()
              if (pool.getHealthmonitorId()) {
                def healthMonitor = healthMonitorSet.find {
                  it.getId() == pool.getHealthmonitorId()
                }
                if (healthMonitor) {
                  listener.healthCheck = new HuaweiCloudLoadBalancerHealthCheck()
                  listener.healthCheck.timeOut = healthMonitor.getTimeout()
                  listener.healthCheck.intervalTime = healthMonitor.getDelay()
                  listener.healthCheck.maxRetries = healthMonitor.getMaxRetries()
                  listener.healthCheck.httpCheckPath = healthMonitor.getUrlPath()
                  listener.healthCheck.httpCheckDomain = healthMonitor.getDomainName()
                }
              }

              def members = []
              membersSet.each {
                if (it.getPoolId() == listener.poolId) {
                  members.add(it)
                }
              }
              listener.targets = members.collect {
                def target = new HuaweiCloudLoadBalancerTarget()
                target.instanceId = it.getId()
                target.port = it.getProtocolPort()
                target.weight = it.getWeight()
                target
              }

              break
            }
          }
        }

        def policies = []
        def listenerId = it.getId()
        policySet.each {
          if (it.getListenerId() == listenerId) {
            policies.add(it)
          }
        }
        def rules = policies.collect() {
          def rule = new HuaweiCloudLoadBalancerRule()
          rule.policyId = it.getId()
          rule.poolId = it.getRedirectPoolId()
          if (rule.poolId) {
            def pool = client.getPool(rule.poolId)
            if (pool.getHealthmonitorId()) {
              def healthMonitor = healthMonitorSet.find {
                it.getId() == pool.getHealthmonitorId()
              }
              if (healthMonitor) {
                rule.healthCheck = new HuaweiCloudLoadBalancerHealthCheck()
                rule.healthCheck.timeOut = healthMonitor.getTimeout()
                rule.healthCheck.intervalTime = healthMonitor.getDelay()
                rule.healthCheck.maxRetries = healthMonitor.getMaxRetries()
                rule.healthCheck.httpCheckPath = healthMonitor.getUrlPath()
                rule.healthCheck.httpCheckDomain = healthMonitor.getDomainName()
              }
            }

            def members = []
            membersSet.each {
              if (it.getPoolId() == listener.poolId) {
                members.add(it)
              }
            }
            rule.targets = members.collect {
              def target = new HuaweiCloudLoadBalancerTarget()
              target.instanceId = it.getId()
              target.port = it.getProtocolPort()
              target.weight = it.getWeight()
              target
            }
          }

          // Comment this to reduce API calls
          /*
          def l7Rules = client.getAllL7rules(it.getId())
          rule.domain = l7Rules.find {
            it.getType() == "HOST_NAME"
          }?.getValue()
          rule.url = l7Rules.find {
            it.getType() == "PATH"
          }?.getValue()
          */
          rule
        }

        listener.rules = rules
        listener
      }
      loadBalancer.listeners = listeners
      loadBalancer
    }
    return loadBanancerList
  }

  @Override
  boolean handles(OnDemandAgent.OnDemandType type, String cloudProvider) {
    type == OnDemandAgent.OnDemandType.LoadBalancer && cloudProvider == HuaweiCloudProvider.ID
  }

  @Override
  OnDemandAgent.OnDemandResult handle(ProviderCache providerCache, Map<String, ? extends Object> data) {
    log.info("Enter handle, data = ${data}")
    if (!data.containsKey("loadBalancerId") ||
      !data.containsKey("account") ||
      !data.containsKey("region")  ||
      accountName != data.account ||
      region != data.region) {
      return null
    }

    def loadBalancer =  metricsSupport.readData {
      loadLoadBalancerData(data.loadBalancerId as String)[0]
    }
    if (!loadBalancer) {
      log.info("Can not find loadBalancer ${data.loadBalancerId}")
      return null
    }

    def cacheResult = metricsSupport.transformData {
      buildCacheResult([loadBalancer], null, null)
    }

    def cacheResultAsJson = objectMapper.writeValueAsString(cacheResult.cacheResults)
    def loadBalancerKey = Keys.getLoadBalancerKey(data.loadBalancerId as String, accountName, region)
    if (cacheResult.cacheResults.values().flatten().empty) {
      // Avoid writing an empty onDemand cache record (instead delete any that may have previously existed).
      providerCache.evictDeletedItems ON_DEMAND.ns, [loadBalancerKey]
    } else {
      metricsSupport.onDemandStore {
        def cacheData = new DefaultCacheData(
          loadBalancerKey,
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

    Map<String, Collection<String>> evictions = loadBalancer ? [:] : [
      (LOAD_BALANCERS.ns): [loadBalancerKey]]

    return new OnDemandResult(
      sourceAgentType: getOnDemandAgentType(),
      cacheResult: cacheResult,
      evictions: evictions
    )
  }

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info("Enter LoadBalancerCacheingAgent loadData ")

    def loadBalancerSet = loadLoadBalancerData()
    log.info("Total loadBanancre Number = ${loadBalancerSet.size()} in ${agentType}")
    def toEvictOnDemandCacheData = []
    def toKeepOnDemandCacheData = []

    Long start = System.currentTimeMillis()
    def loadBalancerKeys = loadBalancerSet.collect {
      Keys.getLoadBalancerKey(it.id, credentials.name, region)
    } as Set<String>

    def pendingOnDemandRequestKeys = providerCache
      .filterIdentifiers(
      ON_DEMAND.ns,
      Keys.getLoadBalancerKey("*", credentials.name, region))
      .findAll { loadBalancerKeys.contains(it) }

    def pendingOnDemandRequestsForloadBalancer = providerCache.getAll(ON_DEMAND.ns, pendingOnDemandRequestKeys)
    pendingOnDemandRequestsForloadBalancer.each {
      if (it.attributes.cacheTime < start && it.attributes.processedCount > 0) {
        toEvictOnDemandCacheData << it
      } else {
        toKeepOnDemandCacheData << it
      }
    }

    CacheResult result = buildCacheResult(loadBalancerSet, toKeepOnDemandCacheData, toEvictOnDemandCacheData)

    result.cacheResults[ON_DEMAND.ns].each {
      it.attributes.processedTime = System.currentTimeMillis()
      it.attributes.processedCount = (it.attributes.processedCount ?: 0) + 1
    }

    /*
    result.cacheResults.each { String namespace, Collection<CacheData> caches->
      log.info "namespace $namespace"
      caches.each{
        log.info "attributes: $it.attributes, relationships: $it.relationships"
      }
    }*/
    return result
  }

  private CacheResult buildCacheResult(Collection<HuaweiCloudLoadBalancer> loadBalancerSet,
                                       Collection<CacheData> toKeepOnDemandCacheData,
                                       Collection<CacheData> toEvictOnDemandCacheData) {
    log.info "Start build cache for $agentType"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Collection<String>> evictions = toEvictOnDemandCacheData ? [(ON_DEMAND.ns):toEvictOnDemandCacheData*.id] : [:]

    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace->[:].withDefault {id->new MutableCacheData(id as String)}
    }

    loadBalancerSet.each {
      Moniker moniker = namer.deriveMoniker it
      def applicationName = moniker.app
      if (applicationName == null) {
        return  //=continue
      }

      def loadBalancerKey = Keys.getLoadBalancerKey(it.id, accountName, region)
      def appKey = Keys.getApplicationKey(applicationName)
      //List<String> instanceKeys = []

      // application
      def applications = namespaceCache[APPLICATIONS.ns]
      applications[appKey].attributes.name = applicationName
      applications[appKey].relationships[LOAD_BALANCERS.ns].add(loadBalancerKey)

      // compare onDemand
      //def onDemandLoadBalancerCache = toKeepOnDemandCacheData.find {
      //  it.id == loadBalancerKey
      //}
      def onDemandLoadBalancerCache = false
      if (onDemandLoadBalancerCache) {
        //mergeOnDemandCache(onDemandLoadBalancerCache, namespaceCache)
      } else {
        // LoadBalancer
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.application = applicationName
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.name = it.name
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.region = it.region
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.id = it.id
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalancerId = it.loadBalancerId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.accountName = accountName
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.subnetId = it.subnetId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.vpcId = it.vpcId
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.createTime = it.createTime
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.loadBalancerVip = it.loadBalancerVip
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners = [] as List<HuaweiCloudLoadBalancerListener>
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.pools = [] as List<HuaweiCloudLoadBalancerPool>
        it.listeners.each {
          def listener = new HuaweiCloudLoadBalancerListener()
          listener.copyListener(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.listeners.add(listener)
        }
        it.pools.each {
          def pool = new HuaweiCloudLoadBalancerPool()
          pool.copyPool(it)
          namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].attributes.pools.add(pool)
        }
        namespaceCache[LOAD_BALANCERS.ns][loadBalancerKey].relationships[APPLICATIONS.ns].add(appKey)
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

  def mergeOnDemandCache(CacheData onDemandLoadBalancerCache, Map<String, Map<String, CacheData>> namespaceCache) {
    Map<String, List<MutableCacheData>> onDemandCache = objectMapper.readValue(
      onDemandLoadBalancerCache.attributes.cacheResults as String,
      new TypeReference<Map<String, List<MutableCacheData>>>() {})

    onDemandCache.each { String namespace, List<MutableCacheData> cacheDataList ->
      if (namespace != 'onDemand') {
        cacheDataList.each {
          def existingCacheData = namespaceCache[namespace][it.id]
          if (!existingCacheData) {
            namespaceCache[namespace][it.id] = it
          } else {
            existingCacheData.attributes.putAll(it.attributes)
            it.relationships.each { String relationshipName, Collection<String> relationships ->
              existingCacheData.relationships[relationshipName].addAll(relationships)
            }
          }
        }
      }
    }
  }

  @Override
  Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    return []
  }

}
