package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.netflix.spinnaker.clouddriver.model.Image

class HuaweiCloudImage implements Image {
  String name
  String region
  String type
  String createdTime
  String imageId
  String osPlatform

  @Override
  @JsonIgnore
  String getId() {
    return imageId
  }
}
