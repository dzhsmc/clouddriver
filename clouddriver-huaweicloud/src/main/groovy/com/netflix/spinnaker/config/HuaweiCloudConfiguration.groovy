package com.netflix.spinnaker.config

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.huaweicloud.config.HuaweiCloudConfigurationProperties
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudCredentialsInitializer
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableConfigurationProperties
@EnableScheduling
@ConditionalOnProperty('huaweicloud.enabled')
@ComponentScan(["com.netflix.spinnaker.clouddriver.huaweicloud"])
@Import([ HuaweiCloudCredentialsInitializer ])
class HuaweiCloudConfiguration {
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Bean
  @ConfigurationProperties("huaweicloud")
  HuaweiCloudConfigurationProperties huaweicloudConfigurationProperties() {
    new HuaweiCloudConfigurationProperties()
  }
}
