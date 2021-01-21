package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

import groovy.transform.AutoClone
import groovy.transform.Canonical

@AutoClone
@Canonical
class DestroyHuaweiCloudServerGroupDescription extends AbstractHuaweiCloudCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
