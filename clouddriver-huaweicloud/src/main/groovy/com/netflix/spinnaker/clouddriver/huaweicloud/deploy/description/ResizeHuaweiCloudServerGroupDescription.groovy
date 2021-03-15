package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class ResizeHuaweiCloudServerGroupDescription extends AbstractHuaweiCloudCredentialsDescription {
  Capacity capacity
  String serverGroupName
  String region
  String accountName

  static class Capacity {
    Integer min
    Integer max
    Integer desired
  }
}
