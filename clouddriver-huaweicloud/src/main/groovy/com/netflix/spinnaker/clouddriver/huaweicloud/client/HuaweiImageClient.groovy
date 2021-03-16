package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ims.v2.ImsClient
import com.huaweicloud.sdk.ims.v2.model.ImageInfo
import com.huaweicloud.sdk.ims.v2.model.ListImagesRequest;
import com.huaweicloud.sdk.ims.v2.model.ListImagesResponse;
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class HuaweiImageClient {
  private final DEFAULT_LIMIT = 100
  ImsClient client

  HuaweiImageClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://ims." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = ImsClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  def getImages() {
    def marker = ""
    List<ImageInfo> imageAll = []
    try {
      while(true) {
        def request = new ListImagesRequest()
        request.setLimit(DEFAULT_LIMIT)
        request.setImagetype(ListImagesRequest.ImagetypeEnum.PRIVATE)
        request.setDiskFormat(ListImagesRequest.DiskFormatEnum.ZVHD2)
        if (marker) {
          request.setMarker(marker)
        }
        def resp = client.listImages(request)
        if(resp == null || resp.getImages() == null || resp.getImages().size() == 0) {
          break
        }
        def images = resp.getImages()
        imageAll.addAll(images)
        marker = images[images.size() - 1].getId()
      }
      return imageAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

}
