package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical


@Canonical
class HuaweiCloudLoadBalancerPool {
  String poolId
  String poolName

  void copyPool(HuaweiCloudLoadBalancerPool pool) {
    if (pool == null) {
      return
    }
    this.poolId = pool.poolId
    this.poolName = pool.poolName
  }
}
