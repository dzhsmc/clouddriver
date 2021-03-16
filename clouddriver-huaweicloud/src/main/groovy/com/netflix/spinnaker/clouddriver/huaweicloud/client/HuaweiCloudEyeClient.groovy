package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.HuaweiCloudDeployDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.ces.v1.CesClient
import com.huaweicloud.sdk.ces.v1.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class HuaweiCloudEyeClient {
  CesClient client

  HuaweiCloudEyeClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://ces." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = CesClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  MetricAlarms getAlarm(String alarmId) {
    try {
      def req = new ShowAlarmRequest()
      req.setAlarmId(alarmId)
      def resp = client.showAlarm(req)
      def alarms = resp.getMetricAlarms()
      if (alarms.size() == 0) {
        throw new HuaweiCloudOperationException("alarm $alarmId  not found")
      }
      alarms[0]
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def createAlarm(MetricAlarms sourceAlarm, String asgId) {
    try {
      def request = new CreateAlarmRequest()
      def body = new CreateAlarmRequestBody()

      def alarmName = "spinnaker-" + new Date().time.toString() + "-" + asgId
      body.setAlarmName(alarmName)
      body.setAlarmDescription(sourceAlarm.getAlarmDescription())
      def metric = sourceAlarm.getMetric()
      def demintions = metric.getDimensions()
      if (demintions.size() > 0) {
        def dimention = demintions[0]
        dimention.setValue(asgId)
        demintions = [dimention]
        metric.setDimensions(demintions)
      }
      body.setMetric(metric)
      body.setCondition(sourceAlarm.getCondition())
      body.setAlarmActions(sourceAlarm.getAlarmActions())
      request.setBody(body)
      def response = client.createAlarm request
      response
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

}
