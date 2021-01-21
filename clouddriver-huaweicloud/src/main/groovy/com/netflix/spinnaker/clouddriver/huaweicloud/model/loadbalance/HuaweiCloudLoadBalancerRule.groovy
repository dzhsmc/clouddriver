package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HuaweiCloudLoadBalancerRule {
  String policyId
  String poolId
  String domain
  String url
  HuaweiCloudLoadBalancerHealthCheck healthCheck
  List<HuaweiCloudLoadBalancerTarget> targets
}
