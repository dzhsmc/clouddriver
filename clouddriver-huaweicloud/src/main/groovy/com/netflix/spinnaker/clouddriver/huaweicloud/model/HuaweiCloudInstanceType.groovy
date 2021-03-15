package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.netflix.spinnaker.clouddriver.model.InstanceType
import groovy.transform.Canonical

@Canonical
class HuaweiCloudInstanceType implements InstanceType {
  String name
  String region
  String account
  Integer cpu
  Integer mem
  String instanceFamily
}
