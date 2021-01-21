package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.netflix.spinnaker.clouddriver.model.Subnet
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider

class HuaweiCloudSubnet implements Subnet {
  final String type = HuaweiCloudProvider.ID
  String name
  String id
  String account
  String region
  String vpcId
  String cidrBlock
  String purpose
}
