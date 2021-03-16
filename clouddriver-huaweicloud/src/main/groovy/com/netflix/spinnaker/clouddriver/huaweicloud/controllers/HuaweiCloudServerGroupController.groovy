package com.netflix.spinnaker.clouddriver.huaweicloud.controllers

import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import com.netflix.spinnaker.clouddriver.huaweicloud.security.HuaweiCloudNamedAccountCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/*
curl -X GET \
  'http://localhost:7002/applications/myapp/clusters/test/myapp-dev/huaweicloud/serverGroups/myapp-dev-v007/scalingActivities?region=ap-guangzhou' \
  -H 'cache-control: no-cache'
*/

@RestController
@RequestMapping("/applications/{application}/clusters/{account}/{clusterName}/huaweicloud/serverGroups/{serverGroupName}")
class HuaweiCloudServerGroupController {
  //final static int MAX_SCALING_ACTIVITIES = 500
  final static int MAX_SCALING_ACTIVITIES = 100

  @Autowired
  AccountCredentialsProvider accountCredentialsProvider

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  @RequestMapping(value = "/scalingActivities", method = RequestMethod.GET)
  ResponseEntity getScalingActivities(
    @PathVariable String account,
    @PathVariable String serverGroupName,
    @RequestParam(value = "region", required = true) String region) {
    def credentials = accountCredentialsProvider.getCredentials(account)
    if (!(credentials instanceof HuaweiCloudNamedAccountCredentials)) {
      return new ResponseEntity(
        [message: "${account} is not huaweicloud credential type"],
        HttpStatus.BAD_REQUEST
      )
    }
    def serverGroup = huaweicloudClusterProvider.getServerGroup(account, region, serverGroupName, false)
    String autoScalingGroupId = serverGroup.asg.autoScalingGroupId
    def client = new HuaweiAutoScalingClient(
      credentials.credentials.accessKeyId,
      credentials.credentials.accessSecretKey,
      region
    )
    def scalingActivities = client.getAutoScalingActivitiesByAsgId(autoScalingGroupId, MAX_SCALING_ACTIVITIES)
    return new ResponseEntity(scalingActivities, HttpStatus.OK)
  }
}
