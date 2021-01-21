package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description

class TerminateHuaweiCloudInstancesDescription extends AbstractHuaweiCloudCredentialsDescription {
  String serverGroupName
  List<String> instanceIds
  String region
  String accountName
}
