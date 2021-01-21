package com.netflix.spinnaker.clouddriver.huaweicloud.cache

import com.google.common.base.CaseFormat
import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.cache.KeyParser
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
@Component("HuaweiCloudKeys")
class Keys implements KeyParser {

  static enum Namespace {
    APPLICATIONS,
    CLUSTERS,
    HEALTH_CHECKS,
    LAUNCH_CONFIGS,
    IMAGES,
    NAMED_IMAGES,
    INSTANCES,
    INSTANCE_TYPES,
    KEY_PAIRS,
    LOAD_BALANCERS,
    NETWORKS,
    SECURITY_GROUPS,
    SERVER_GROUPS,
    SUBNETS,
    ON_DEMAND,

    public final String ns

    private Namespace() {
      ns = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name())
    }

    String toString() {
      ns
    }
  }

  @Override
  String getCloudProvider() {
    return HuaweiCloudProvider.ID
  }

  @Override
  Map<String, String> parseKey(String key) {
    return parse(key)
  }

  @Override
  Boolean canParseType(String type) {
    return Namespace.values().any { it.ns == type }
  }

  @Override
  Boolean canParseField(String field) {
    return false
  }

  static Map<String, String> parse(String key) {
    // todo
    def parts = key.split(':')

    if (parts.length < 2 || parts[0] != HuaweiCloudProvider.ID) {
      return null
    }

    def result = [provider: parts[0], type: parts[1]]

    switch (result.type) {
      case Namespace.CLUSTERS.ns:
        def names = Names.parseName(parts[4])
        result << [
          application: parts[3],
          account    : parts[2],
          name       : parts[4],
          cluster    : parts[4],
          stack      : names.stack,
          detail     : names.detail
        ]
        break
      case Namespace.IMAGES.ns:
        result << [
          account: parts[2],
          region : parts[3],
          imageId: parts[4]
        ]
        break
      case Namespace.NAMED_IMAGES.ns:
        result << [
          account  : parts[2],
          imageName: parts[3]
        ]
        break
      case Namespace.SECURITY_GROUPS.ns:
        def names = Names.parseName(parts[2])
        result << [
          application: names.app,
          name       : parts[2],
          account    : parts[3],
          region     : parts[4],
          id         : parts[5]
        ]
        break
      case Namespace.NETWORKS.ns:
        result << [
          account: parts[2],
          region : parts[3],
          id     : parts[4]
        ]
        break
      case Namespace.SUBNETS.ns:
        result << [
          account: parts[2],
          region : parts[3],
          id     : parts[4]
        ]
        break
      case Namespace.LOAD_BALANCERS.ns:
        result << [
          account: parts[2],
          region : parts[3],
          id     : parts[4]
        ]
        break
      case Namespace.SERVER_GROUPS.ns:
        result << [
          account: parts[2],
          region : parts[3],
          cluster: parts[4],
          name   : parts[5]
        ]
        break
      default:
        return null
        break
    }

    result
  }

  static String getApplicationKey(String application) {
    "$HuaweiCloudProvider.ID:$Namespace.APPLICATIONS:${application.toLowerCase()}"
  }

  static String getClusterKey(String clusterName, String application, String account) {
    "$HuaweiCloudProvider.ID:$Namespace.CLUSTERS:$account:${application?.toLowerCase()}:$clusterName"
  }

  static String getServerGroupKey(String serverGroupName, String account, String region) {
    Names names = Names.parseName(serverGroupName)
    return getServerGroupKey(names.cluster, names.group, account, region)
  }

  static String getServerGroupKey(String cluster, String serverGroupName, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.SERVER_GROUPS:$account:$region:$cluster:$serverGroupName"
  }

  static String getInstanceKey(String instanceId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.INSTANCES:$account:$region:$instanceId"
  }

  static String getImageKey(String imageId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.IMAGES:$account:$region:$imageId"
  }

  static String getNamedImageKey(String imageName, String account) {
    "$HuaweiCloudProvider.ID:$Namespace.NAMED_IMAGES:$account:$imageName"
  }

  static String getKeyPairKey(String keyId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.KEY_PAIRS:$account:$region:$keyId"
  }

  static String getInstanceTypeKey(String account, String region, String instanceType) {
    "$HuaweiCloudProvider.ID:$Namespace.INSTANCE_TYPES:$account:$region:$instanceType"
  }

  static String getLoadBalancerKey(String loadBalancerId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.LOAD_BALANCERS:$account:$region:$loadBalancerId"
  }

  static String getSecurityGroupKey(String securityGroupId, String securityGroupName, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.SECURITY_GROUPS:$securityGroupName:$account:$region:$securityGroupId"
  }

  static String getNetworkKey(String networkId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.NETWORKS:$account:$region:$networkId"
  }

  static String getSubnetKey(String subnetId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.SUBNETS:$account:$region:$subnetId"
  }

  static String getTargetHealthKey(String loadBalancerId, String listenerId, String poolId, String instanceId, String account, String region) {
    "$HuaweiCloudProvider.ID:$Namespace.HEALTH_CHECKS:$account:$region:$loadBalancerId:$listenerId:$poolId:$instanceId"
  }
}
