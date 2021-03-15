package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.netflix.spinnaker.clouddriver.model.Network
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider

class HuaweiCloudNetwork implements Network{
  String cloudProvider = HuaweiCloudProvider.ID
  String id
  String name
  String account
  String region
  String cidrBlock
}
