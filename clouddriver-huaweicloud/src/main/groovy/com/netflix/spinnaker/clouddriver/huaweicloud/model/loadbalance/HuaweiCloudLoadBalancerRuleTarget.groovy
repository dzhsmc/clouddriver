package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HuaweiCloudLoadBalancerRuleTarget {
  String poolId
  String domain
  String url
  List<HuaweiCloudLoadBalancerTarget> targets
}
