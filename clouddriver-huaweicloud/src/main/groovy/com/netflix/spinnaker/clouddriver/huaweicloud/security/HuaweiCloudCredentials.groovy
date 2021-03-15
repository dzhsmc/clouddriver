package com.netflix.spinnaker.clouddriver.huaweicloud.security

class HuaweiCloudCredentials {
  final String accessKeyId
  final String accessSecretKey

  HuaweiCloudCredentials(String accessKeyId, String accessSecretKey)
  {
    this.accessKeyId = accessKeyId
    this.accessSecretKey = accessSecretKey
  }
}
