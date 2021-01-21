package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DeleteHuaweiCloudLoadBalancerDescription extends AbstractHuaweiCloudCredentialsDescription {
  String application
  String accountName
  String region
  String loadBalancerId
  List<HuaweiCloudLoadBalancerListener> listener  //listeners
}
