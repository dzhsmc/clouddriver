package com.netflix.spinnaker.clouddriver.huaweicloud.provider.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.CachingAgent
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.HuaweiCloudInfrastructureProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudImageCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudInstanceCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudInstanceTypeCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudKeyPairCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudLoadBalancerInstanceStateCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudNetworkCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudServerGroupCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudLoadBalancerCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudSecurityGroupCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent.HuaweiCloudSubnetCachingAgent
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import com.netflix.spinnaker.config.HuaweiCloudConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@Configuration
@Import(HuaweiCloudConfiguration)
@EnableConfigurationProperties
class HuaweiCloudInfrastructureProviderConfig {
  @Autowired
  Registry registry

  @Bean
  @DependsOn('huaweicloudNamedAccountCredentials')
  HuaweiCloudInfrastructureProvider huaweicloudInfrastructureProvider(
    AccountCredentialsRepository accountCredentialsRepository,
    ObjectMapper objectMapper,
    Registry registry) {

    List<CachingAgent> agents = []
    def allAccounts = accountCredentialsRepository.all.findAll {
      it instanceof HuaweiCloudNamedAccountCredentials
    } as Collection<HuaweiCloudNamedAccountCredentials>

    // enable multiple accounts and multiple regions in each account
    allAccounts.each { HuaweiCloudNamedAccountCredentials credential ->
      credential.regions.each { region ->
        agents << new HuaweiCloudServerGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name)

        agents << new HuaweiCloudInstanceTypeCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudKeyPairCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudImageCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudInstanceCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudLoadBalancerCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new HuaweiCloudSecurityGroupCachingAgent(
          credential,
          objectMapper,
          registry,
          region.name
        )

        agents << new HuaweiCloudNetworkCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudSubnetCachingAgent(
          credential,
          objectMapper,
          region.name
        )

        agents << new HuaweiCloudLoadBalancerInstanceStateCachingAgent(
          credential,
          objectMapper,
          region.name
        )
      }
    }
    return new HuaweiCloudInfrastructureProvider(agents)
  }
}
