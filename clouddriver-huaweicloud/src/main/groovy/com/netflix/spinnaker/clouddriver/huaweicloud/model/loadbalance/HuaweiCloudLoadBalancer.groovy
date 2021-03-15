package com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance


import com.netflix.spinnaker.clouddriver.model.LoadBalancer
import com.netflix.spinnaker.clouddriver.model.LoadBalancerServerGroup
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.moniker.Moniker


class HuaweiCloudLoadBalancer implements LoadBalancer, HuaweiCloudBasicResource{
  final String cloudProvider = HuaweiCloudProvider.ID
  final String type = HuaweiCloudProvider.ID
  String application
  String accountName
  String region

  String id
  String name
  String loadBalancerId
  String loadBalancerName
  String subnetId
  String vpcId
  String createTime
  String loadBalancerVip
  List<HuaweiCloudLoadBalancerListener> listeners
  List<HuaweiCloudLoadBalancerPool> pools

  Set<LoadBalancerServerGroup> serverGroups = []

  @Override
  String getAccount() {
    accountName
  }

  @Override
  Moniker getMoniker() {
    return NamerRegistry.lookup()
      .withProvider(HuaweiCloudProvider.ID)
      .withAccount(accountName)
      .withResource(HuaweiCloudBasicResource)
      .deriveMoniker(this)
  }

  @Override
  String getMonikerName() {
    name
  }

  @Override
  boolean equals(Object o) {
    if (!(o instanceof HuaweiCloudLoadBalancer)) {
      return false
    }
    HuaweiCloudLoadBalancer other = (HuaweiCloudLoadBalancer)o
    other.getAccount() == this.getAccount() && other.getName() == this.getName() && other.getType() == this.getType() && other.getId() == this.getId() && other.getRegion() == this.getRegion()
  }

  @Override
  int hashCode() {
    //getAccount().hashCode() + getId().hashCode() + getType().hashCode() + region.hashCode()
    getId().hashCode() + getType().hashCode()
  }
}
