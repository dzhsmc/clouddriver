package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerRule
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class UpsertHuaweiCloudLoadBalancerDescription extends AbstractHuaweiCloudCredentialsDescription {
  String application
  String accountName
  String region

  String loadBalancerId
  String loadBalancerName
  String vpcId
  String subnetId

  //listener, rule, target
  List<HuaweiCloudLoadBalancerListener> listener  //listeners
}
