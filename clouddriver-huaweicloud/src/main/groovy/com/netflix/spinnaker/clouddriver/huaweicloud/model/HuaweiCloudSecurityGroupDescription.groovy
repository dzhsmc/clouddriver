package com.netflix.spinnaker.clouddriver.huaweicloud.model

class HuaweiCloudSecurityGroupDescription {
  String securityGroupId
  String securityGroupName
  String securityGroupDesc
  List<HuaweiCloudSecurityGroupRule> inRules
  List<HuaweiCloudSecurityGroupRule> outRules
  long lastReadTime
}
