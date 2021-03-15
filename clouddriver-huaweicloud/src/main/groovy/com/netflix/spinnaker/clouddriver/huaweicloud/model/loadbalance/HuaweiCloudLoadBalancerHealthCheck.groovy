package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical

@Canonical
class HuaweiCloudLoadBalancerHealthCheck {
  Integer timeOut;
  Integer intervalTime;
  Integer maxRetries;
  String httpCheckPath;
  String httpCheckDomain;

  void copyHealthCheck(HuaweiCloudLoadBalancerHealthCheck health) {
    if (health != null) {
      this.timeOut = health.timeOut
      this.intervalTime = health.intervalTime
      this.maxRetries = health.maxRetries
      this.httpCheckPath = health.httpCheckPath
      this.httpCheckDomain = health.httpCheckDomain
    }
  }
}
