package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ecs.v2.EcsClient
import com.huaweicloud.sdk.ecs.v2.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Slf4j
class HuaweiElasticCloudServerClient {
  private final DEFAULT_LIMIT = 100
  EcsClient client

  HuaweiElasticCloudServerClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://ecs." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = EcsClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  def terminateInstances(List<String> instanceIds) {
    try {
      def request = new BatchStopServersRequest()
      def body = new BatchStopServersRequestBody()
      def ops = new BatchStopServersOption()
      ops.setType(BatchStopServersOption.TypeEnum.SOFT)
      def servers = instanceIds.collect{
        def server = new ServerId().withId(it)
        server
      }
      ops.setServers(servers)
      body.setOsStop(ops)
      request.setBody(body)
      client.batchStopServers(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  def rebootInstances(List<String> instanceIds) {
    try {
      def request = new BatchRebootServersRequest()
      def body = new BatchRebootServersRequestBody()
      def ops = new BatchRebootSeversOption()
      ops.setType(BatchRebootSeversOption.TypeEnum.SOFT)
      def servers = instanceIds.collect{
        def server = new ServerId().withId(it)
        server
      }
      ops.setServers(servers)
      body.setReboot(ops)
      request.setBody(body)
      client.batchRebootServers(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  def getInstanceTypes() {
    try {
      def request = new ListFlavorsRequest()
      def response = client.listFlavors(request)
      response.getFlavors()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def getKeyPairs() {
    try {
      def request = new NovaListKeypairsRequest()
      def response = client.novaListKeypairs(request)
      response.getKeypairs()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def getInstances() {
    def startNumber = 0
    List<ServerDetail> instanceAll = []
    try {
      while(true) {
        def req = new ListServersDetailsRequest().withLimit(DEFAULT_LIMIT).withOffset(startNumber)
        def resp = client.listServersDetails(req)
        if(resp == null || resp.getServers() == null || resp.getServers().size() == 0) {
          break
        }
        instanceAll.addAll(resp.getServers())
        startNumber += DEFAULT_LIMIT
      }
      return instanceAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def getInstanceTags(String instanceId) {
    try {
      def request = new ShowServerTagsRequest().withServerId(instanceId)
      def response = client.showServerTags(request)
      response.getTags()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  static Date ConvertIsoDateTime(String isoDateTime) {
    try {
      DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME
      TemporalAccessor accessor = timeFormatter.parse(isoDateTime)
      Date date = Date.from(Instant.from(accessor))
      return date
    } catch (Exception e) {
      log.warn "convert time error ${e.toString()}"
      return null
    }
  }

}
