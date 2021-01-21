package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.netflix.spinnaker.clouddriver.model.KeyPair

class HuaweiCloudKeyPair implements KeyPair{
  String account
  String region
  String keyId
  String keyName
  String keyFingerprint
}
