package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.InstanceProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstanceHealth
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudTargetHealth
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTargetHealth

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.*

import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class HuaweiCloudInstanceProvider implements InstanceProvider<HuaweiCloudInstance, String> {
  final String cloudProvider = HuaweiCloudProvider.ID

  @Autowired
  ObjectMapper objectMapper

  @Autowired
  HuaweiCloudProvider huaweiCloudProvider

  @Autowired
  Cache cacheView

  @Override
  HuaweiCloudInstance getInstance(String account, String region, String id) {
    def key = Keys.getInstanceKey id, account, region

    cacheView.getAll(
      INSTANCES.ns,
      [key],
      RelationshipCacheFilter.include(
        LOAD_BALANCERS.ns,
        SERVER_GROUPS.ns,
        CLUSTERS.ns
      )
    )?.findResult { CacheData cacheData ->
      instanceFromCacheData(account, region, cacheData)
    }
  }

  HuaweiCloudInstance instanceFromCacheData(String account, String region, CacheData cacheData) {
    HuaweiCloudInstance instance = objectMapper.convertValue cacheData.attributes.instance, HuaweiCloudInstance

    def serverGroupName = instance.serverGroupName
    def serverGroupCache = cacheView.get(SERVER_GROUPS.ns, Keys.getServerGroupKey(serverGroupName, account, region))
    def asgInfo = serverGroupCache?.attributes?.asg as Map
    // Scaling instance states
    def asgInstances = asgInfo?.get("instances") as List
    def asgInstance = asgInstances.find {
      it["instance_id"] = instance.name
    }
    // LB targets states
    def lbInfos = asgInfo?.get("forwardLoadBalancerSet") as List
    if (lbInfos) {
      for (lbInfo in lbInfos) {
        def lbId = lbInfo["loadBalancerId"] as String
        def listenerId = lbInfo["listenerId"] as String
        def poolId = lbInfo['poolId'] as String
        def lbHealthKey = Keys.getTargetHealthKey(
          lbId, listenerId, poolId, instance.name, account, region)
        def lbHealthCache = cacheView.get(HEALTH_CHECKS.ns, lbHealthKey)
        def loadBalancerTargetHealth = lbHealthCache?.attributes?.targetHealth as HuaweiCloudLoadBalancerTargetHealth
        if (loadBalancerTargetHealth) {
          def targetHealth = new HuaweiCloudTargetHealth(loadBalancerTargetHealth.healthStatus)
          def healthStatus = targetHealth.targetHealthStatus
          targetHealth.loadBalancers.add(new HuaweiCloudTargetHealth.LBHealthSummary(
            loadBalancerName: lbId,
            state: healthStatus.toServiceStatus()
          ))
          instance.targetHealths.add(targetHealth)
        } else {
          // if server group has lb, but can't get lb health check result for instance in server group
          // assume the target health check result is UNKNOWN
          instance.targetHealths.add(new HuaweiCloudTargetHealth())
        }
      }
    } else if (asgInstance && asgInstance["life_cycle_state"].equals("INSERVICE") && asgInstance["health_status"].equals("NORMAL")) {
      instance.instanceHealth = new HuaweiCloudInstanceHealth(instanceStatus: "NORMAL")
    }
    instance
  }

  @Override
  String getConsoleOutput(String account, String region, String id) {
    return null
  }
}
