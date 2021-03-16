package com.netflix.spinnaker.clouddriver.huaweicloud.provider.agent

import com.netflix.spinnaker.cats.agent.AgentDataType
import com.netflix.spinnaker.cats.agent.CacheResult
import com.netflix.spinnaker.cats.agent.DefaultCacheResult
import com.netflix.spinnaker.cats.cache.CacheData
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiImageClient
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudImage
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.MutableCacheData
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.IMAGES
import static com.netflix.spinnaker.clouddriver.huaweicloud.cache.Keys.Namespace.NAMED_IMAGES

@Slf4j
@InheritConstructors
class HuaweiCloudImageCachingAgent extends AbstractHuaweiCloudCachingAgent {

  final Set<AgentDataType> providedDataTypes = [
    AUTHORITATIVE.forType(IMAGES.ns)
  ] as Set

  @Override
  CacheResult loadData(ProviderCache providerCache) {
    log.info "start load image data"

    Map<String, Collection<CacheData>> cacheResults = [:]
    Map<String, Map<String, CacheData>> namespaceCache = [:].withDefault {
      namespace -> [:].withDefault { id -> new MutableCacheData(id as String) }
    }
    Map<String, Collection<String>> evictions = [:].withDefault {
      namespace -> []
    }

    HuaweiImageClient imsClient = new HuaweiImageClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )

    def result = imsClient.getImages()

    result.each {
      def huaweicloudImage = new HuaweiCloudImage(
        region: this.region,
        name: it.getName(),
        imageId: it.getId(),
        type: it.getImagetype().toString(),
        osPlatform: it.getOsType().toString(),
        createdTime: it.getCreatedAt()
      )

      def images = namespaceCache[IMAGES.ns]
      def namedImages = namespaceCache[NAMED_IMAGES.ns]
      def imageKey = Keys.getImageKey huaweicloudImage.id, this.accountName, this.region
      def namedImageKey = Keys.getNamedImageKey huaweicloudImage.name, this.accountName
      images[imageKey].attributes.image = huaweicloudImage
      images[imageKey].relationships[NAMED_IMAGES.ns].add namedImageKey

      def originImageCache = providerCache.get(IMAGES.ns, imageKey)
      if (originImageCache) {
        def imageNames = originImageCache.relationships[NAMED_IMAGES.ns]
        if (imageNames && imageNames[0] != namedImageKey) {
          evictions[NAMED_IMAGES.ns].addAll(originImageCache.relationships[NAMED_IMAGES.ns])
        }
      }

      namedImages[namedImageKey].attributes.imageName = huaweicloudImage.name
      namedImages[namedImageKey].attributes.type = huaweicloudImage.type
      namedImages[namedImageKey].attributes.osPlatform = huaweicloudImage.osPlatform
      namedImages[namedImageKey].attributes.createdTime = huaweicloudImage.createdTime
      namedImages[namedImageKey].attributes.imageId = huaweicloudImage.imageId
      namedImages[namedImageKey].relationships[IMAGES.ns].add imageKey
      null
    }

    namespaceCache.each { String namespace, Map<String, CacheData> cacheDataMap ->
      cacheResults[namespace] = cacheDataMap.values()
    }

    CacheResult defaultCacheResult = new DefaultCacheResult(cacheResults, evictions)
    log.info 'finish loads image data.'
    log.info "Caching ${namespaceCache[IMAGES.ns].size()} items in $agentType"
    defaultCacheResult
  }
}

