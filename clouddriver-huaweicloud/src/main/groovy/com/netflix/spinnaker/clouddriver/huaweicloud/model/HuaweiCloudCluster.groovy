package com.netflix.spinnaker.clouddriver.huaweicloud.model


import com.netflix.spinnaker.clouddriver.model.Cluster
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancer
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

@CompileStatic
@EqualsAndHashCode(includes = ["name", "accountName"])
class HuaweiCloudCluster implements Cluster {
  final String type = HuaweiCloudProvider.ID
  String name
  String accountName
  Set<HuaweiCloudServerGroup> serverGroups = Collections.synchronizedSet(new HashSet<HuaweiCloudServerGroup>())
  Set<HuaweiCloudLoadBalancer> loadBalancers = Collections.synchronizedSet(new HashSet<HuaweiCloudLoadBalancer>())
}
