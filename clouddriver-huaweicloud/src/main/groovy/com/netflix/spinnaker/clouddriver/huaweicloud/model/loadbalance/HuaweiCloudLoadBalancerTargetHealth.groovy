package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

class HuaweiCloudLoadBalancerTargetHealth {
  String instanceId
  Integer port
  Boolean healthStatus
  String loadBalancerId
  String listenerId
  String poolId
}
