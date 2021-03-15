package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

class TerminateAndDecrementHuaweiCloudServerGroupDescription extends AbstractHuaweiCloudCredentialsDescription {
  String serverGroupName
  String region
  String instance
}
