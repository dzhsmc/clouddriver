package com.netflix.spinnaker.clouddriver.huaweicloud.security

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsRepository
import com.netflix.spinnaker.clouddriver.security.CredentialsInitializerSynchronizable
import com.netflix.spinnaker.clouddriver.huaweicloud.config.HuaweiCloudConfigurationProperties
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Slf4j
@Configuration
class HuaweiCloudCredentialsInitializer{
  @Bean
  List<HuaweiCloudNamedAccountCredentials> huaweicloudNamedAccountCredentials(
    HuaweiCloudConfigurationProperties huaweicloudConfigurationProperties,
    AccountCredentialsRepository accountCredentialsRepository,
    String clouddriverUserAgentApplicationName
  ) {
    def huaweicloudAccounts = []
    huaweicloudConfigurationProperties.accounts.each {
      HuaweiCloudConfigurationProperties.ManagedAccount managedAccount ->
      try {
        def huaweicloudAccount = new HuaweiCloudNamedAccountCredentials(
          managedAccount.name,
          managedAccount.environment ?: managedAccount.name,
          managedAccount.accountType ?: managedAccount.name,
          managedAccount.accessKeyId,
          managedAccount.accessSecretKey,
          managedAccount.regions,
          clouddriverUserAgentApplicationName
        )
        huaweicloudAccounts << (accountCredentialsRepository.save(managedAccount.name, huaweicloudAccount)
        as HuaweiCloudNamedAccountCredentials)
      } catch (e) {
        log.error("Could not load account ${managedAccount.name} for HuaweiCloud.", e)
      }
    }
  }
}
