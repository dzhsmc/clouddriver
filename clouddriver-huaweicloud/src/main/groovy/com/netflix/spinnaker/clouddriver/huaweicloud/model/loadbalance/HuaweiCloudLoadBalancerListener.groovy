package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance

import groovy.transform.Canonical


@Canonical
class HuaweiCloudLoadBalancerListener {
  String listenerId
  String listenerName
  String protocol
  Integer port

  //target, tcp/udp 4 layer
  String poolId
  HuaweiCloudLoadBalancerHealthCheck healthCheck
  List<HuaweiCloudLoadBalancerTarget> targets

  //rule, http/https 7 layer
  HuaweiCloudLoadBalancerCertificate certificate
  List<HuaweiCloudLoadBalancerRule> rules

  void copyListener(HuaweiCloudLoadBalancerListener listener) {
    if (listener == null) {
      return
    }
    this.listenerId = listener.listenerId
    this.listenerName = listener.listenerName
    this.protocol = listener.protocol
    this.port = listener.port
    this.poolId = listener.poolId
    this.healthCheck = new HuaweiCloudLoadBalancerHealthCheck()
    this.healthCheck.copyHealthCheck(listener.healthCheck)
    this.certificate = new HuaweiCloudLoadBalancerCertificate()
    this.certificate.copyCertificate(listener.certificate)
    this.targets = listener.targets.collect {
      def target = new HuaweiCloudLoadBalancerTarget()
      target.instanceId = it.instanceId
      target.port = it.port
      target.weight = it.weight
      target
    }
    this.rules = listener.rules.collect {
      def rule = new HuaweiCloudLoadBalancerRule()
      rule.policyId = it.policyId
      rule.poolId = it.poolId
      rule.domain = it.domain
      rule.url = it.url
      rule.healthCheck = new HuaweiCloudLoadBalancerHealthCheck()
      rule.healthCheck.copyHealthCheck(it.healthCheck)
      rule.targets = it.targets.collect {
        def target = new HuaweiCloudLoadBalancerTarget()
        target.instanceId = it.instanceId
        target.port = it.port
        target.weight = it.weight
        target
      }
      rule
    }
  }

}
