package com.netflix.spinnaker.clouddriver.huaweicloud.security

import com.netflix.spinnaker.clouddriver.names.NamerRegistry
import com.netflix.spinnaker.clouddriver.security.AccountCredentials
import com.netflix.spinnaker.clouddriver.huaweicloud.config.HuaweiCloudConfigurationProperties
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudBasicResource
import com.netflix.spinnaker.clouddriver.huaweicloud.names.HuaweiCloudBasicResourceNamer
import com.netflix.spinnaker.fiat.model.resources.Permissions
import com.netflix.spinnaker.moniker.Namer
import groovy.transform.Canonical
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j

@Slf4j
@Canonical
@TupleConstructor
class HuaweiCloudNamedAccountCredentials implements AccountCredentials<HuaweiCloudCredentials> {
  final String cloudProvider = HuaweiCloudProvider.ID

  final String name
  final String environment
  final String accountType
  final HuaweiCloudCredentials credentials
  List<HuaweiCloudRegion> regions

  final List<String> requiredGroupMembership
  final Permissions permissions

  Namer namer = new HuaweiCloudBasicResourceNamer()

  HuaweiCloudNamedAccountCredentials(
    String name,
    String environment,
    String accountType,
    String accessKeyId,
    String accessSecretKey,
    List<HuaweiCloudConfigurationProperties.ManagedAccount.Region> regions,
    String clouddriverUserAgentApplicationName
  ){
    this.name = name
    this.environment = environment
    this.accountType = accountType
    this.credentials = new HuaweiCloudCredentials(accessKeyId, accessSecretKey)
    this.regions = buildRegions(regions)
    NamerRegistry.lookup()
      .withProvider(HuaweiCloudProvider.ID)
      .withAccount(name)
      .setNamer(HuaweiCloudBasicResource.class, namer)
  }

  private static List<HuaweiCloudRegion> buildRegions(List<HuaweiCloudConfigurationProperties.ManagedAccount.Region> regions) {
    regions?.collect {new HuaweiCloudRegion(it.name, it.availabilityZones)} ?: new ArrayList<HuaweiCloudRegion>()
  }

  static class HuaweiCloudRegion {
    public final String name
    public final List<String> availabilityZones
    public final List<String> preferredZones

    HuaweiCloudRegion(String name, List<String> availabilityZones) {
      if (name == null) {
        throw new IllegalArgumentException("name must be specified.")
      }
      this.name = name
      this.availabilityZones = availabilityZones
      this.preferredZones = availabilityZones
    }

    String getName() {return name}

    @Override
    boolean equals(Object o) {
      if (this == o) return true
      if (o == null || getClass() != o.getClass()) return false

      HuaweiCloudRegion region = (HuaweiCloudRegion) o

      name.equals(region.name)
    }

    @Override
    int hashCode() {
      name.hashCode()
    }
  }
}
