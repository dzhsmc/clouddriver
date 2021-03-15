package com.netflix.spinnaker.clouddriver.huaweicloud.names

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.names.NamingStrategy
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.moniker.Moniker
import org.springframework.stereotype.Component


@Component
class HuaweiCloudBasicResourceNamer implements NamingStrategy<HuaweiCloudBasicResource> {
  @Override
  String getName() {
    return "huaweicloudAnnotations"
  }

  void applyMoniker(HuaweiCloudBasicResource huaweicloudBasicResource, Moniker moniker) {
  }

  @Override
  Moniker deriveMoniker(HuaweiCloudBasicResource huaweicloudBasicResource) {
    String name = huaweicloudBasicResource.monikerName
    Names parsed = Names.parseName(name)

    Moniker moniker = Moniker.builder()
      .app(parsed.getApp())
      .cluster(parsed.getCluster())
      .detail(parsed.getDetail())
      .stack(parsed.getStack())
      .detail(parsed.getDetail())
      .sequence(parsed.getSequence())
      .build()

    return moniker
  }
}
