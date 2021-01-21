package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

class EnableDisableHuaweiCloudServerGroupDescription extends AbstractHuaweiCloudCredentialsDescription {
  String serverGroupName
  String region
  String accountName
}
