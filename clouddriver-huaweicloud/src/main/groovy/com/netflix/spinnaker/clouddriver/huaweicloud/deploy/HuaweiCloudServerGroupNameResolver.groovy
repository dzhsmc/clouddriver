package com.netflix.spinnaker.clouddriver.huaweicloud.deploy

import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.client.AutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.ElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.moniker.Namer


class HuaweiCloudServerGroupNameResolver extends AbstractServerGroupNameResolver{
  private static final String HUAWEICLOUD_PHASE = "HUAWEICLOUD_DEPLOY"
  private final String accountName
  private final String region
  private final HuaweiCloudClusterProvider huaweicloudClusterProvider
  private final AutoScalingClient autoScalingClient
  private final Namer namer

  HuaweiCloudServerGroupNameResolver(
    String accountName,
    String region,
    HuaweiCloudClusterProvider huaweicloudClusterProvider,
    HuaweiCloudNamedAccountCredentials credentials
  ) {
    this.accountName = accountName
    this.region = region
    this.huaweicloudClusterProvider = huaweicloudClusterProvider
    this.namer = NamerRegistry.lookup()
      .withProvider(HuaweiCloudProvider.ID)
      .withAccount(accountName)
      .withResource(HuaweiCloudBasicResource)
    this.autoScalingClient = new AutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
  }

  @Override
  String getPhase() {
    return HUAWEICLOUD_PHASE
  }

  @Override
  String getRegion() {
    return region
  }

  @Override
  List<AbstractServerGroupNameResolver.TakenSlot> getTakenSlots(String clusterName) {
    def applicationName = Names.parseName(clusterName).app
    def cluster = huaweicloudClusterProvider.getCluster(applicationName, accountName, clusterName)
    if (!cluster) {
      []
    }
    else {
      def autoScalingGroups = autoScalingClient.getAllAutoScalingGroups()
      def serverGroupsInCluster = autoScalingGroups.findAll {
        Names.parseName(it.getScalingGroupName()).cluster == clusterName
      }

      return serverGroupsInCluster.collect {
        def name = it.getScalingGroupName()
        def date = ElasticCloudServerClient.ConvertIsoDateTime(it.getCreateTime().toString())
        new AbstractServerGroupNameResolver.TakenSlot(
            serverGroupName: name,
            sequence: Names.parseName(name).sequence,
            createdTime: date
          )
      }
    }
  }
}
