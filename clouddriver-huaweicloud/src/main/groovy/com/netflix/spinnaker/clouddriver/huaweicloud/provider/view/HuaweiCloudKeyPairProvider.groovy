package com.netflix.spinnaker.clouddriver.huaweicloud.provider.view

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.cats.cache.Cache
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.clouddriver.model.KeyPairProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudKeyPair
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.KEY_PAIRS

@Slf4j
@Component
class HuaweiCloudKeyPairProvider implements KeyPairProvider<HuaweiCloudKeyPair> {

  @Autowired
  Cache cacheView

  private final ObjectMapper objectMapper

  @Autowired
  HuaweiCloudKeyPairProvider(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper
  }

  @Override
  Set<HuaweiCloudKeyPair> getAll() {
    cacheView.getAll(
      KEY_PAIRS.ns,
      cacheView.filterIdentifiers(
        KEY_PAIRS.ns,
        Keys.getKeyPairKey('*','*','*')
      ),
      RelationshipCacheFilter.none()).collect {
      objectMapper.convertValue it.attributes.keyPair, HuaweiCloudKeyPair
    }
  }
}
