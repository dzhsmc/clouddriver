package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HuaweiCloudLoadBalancerTarget {
  String instanceId
  Integer port
  Integer weight
}
