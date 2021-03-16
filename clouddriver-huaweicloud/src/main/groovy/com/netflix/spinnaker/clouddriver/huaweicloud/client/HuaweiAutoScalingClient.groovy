package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.HuaweiCloudDeployDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.as.v1.AsClient
import com.huaweicloud.sdk.as.v1.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class HuaweiAutoScalingClient {
  private final DEFAULT_LIMIT = 100
  private final MAX_TRY_COUNT = 60
  private final REQ_TRY_INTERVAL = 60 * 1000  //MillSeconds
  static String defaultServerGroupTagKey = "spinnaker-server-group-name"
  AsClient client

  HuaweiAutoScalingClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://as." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = AsClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  String deploy(HuaweiCloudDeployDescription description) {
    try {
      // 1. create launch configuration
      def createLaunchConfigurationRequest = buildLaunchConfigurationRequest(description)
      def createLaunchConfigurationResponse = client.createScalingConfig(createLaunchConfigurationRequest)
      String launchConfigurationId = createLaunchConfigurationResponse.getScalingConfigurationId()

      try {
        // 2. create auto scaling group
        def createAutoScalingGroupRequest = buildAutoScalingGroupRequest(description, launchConfigurationId)
        def createAutoScalingGroupResponse = client.createScalingGroup(createAutoScalingGroupRequest)
        def scalingGroupId = createAutoScalingGroupResponse.getScalingGroupId()

        // 3. create tags for scaling group
        def createScalingTagsRequest = new CreateScalingTagsRequest()
        def createScalingTagsRequestBody = new CreateScalingTagsRequestBody()
        createScalingTagsRequestBody.setAction(CreateScalingTagsRequestBody.ActionEnum.CREATE)
        List<TagsSingleValue> tags = []
        def spinnakerTag = new TagsSingleValue().withKey(defaultServerGroupTagKey).withValue(description.serverGroupName)
        tags.add(spinnakerTag)
        if (description.instanceTags) {
          tags.addAll description.instanceTags.collect {
              new TagsSingleValue().withKey(it.key).withValue(it.value)
          }
        }
        createScalingTagsRequestBody.setTags(tags)
        createScalingTagsRequest.setBody(createScalingTagsRequestBody)
        createScalingTagsRequest.setResourceType(CreateScalingTagsRequest.ResourceTypeEnum.SCALING_GROUP_TAG)
        createScalingTagsRequest.setResourceId(scalingGroupId)
        client.createScalingTags(createScalingTagsRequest)

        // 4. enable auto scaling group
        enableAutoScalingGroup(scalingGroupId)
        scalingGroupId
      } catch (ServiceResponseException e) {
        // if create auto scaling group failed, delete launch configuration.
        sleep(5000)  // wait for a while before delete launch configuration
        log.error(e.toString())
        def request = new DeleteScalingConfigRequest()
        request.setScalingConfigurationId(launchConfigurationId)
        client.deleteScalingConfig(request)
        throw e
      }
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  private static def buildLaunchConfigurationRequest(HuaweiCloudDeployDescription description) {
    def request = new CreateScalingConfigRequest()
    def body = new CreateScalingConfigRequestBody()
    def instanceConfig = new InstanceConfig()

    def launchConfigurationName = description.serverGroupName
    body.setScalingConfigurationName(launchConfigurationName)

    // image
    instanceConfig.setImageRef(description.imageId)

    // flavor
    if (description.instanceType) {
      instanceConfig.setFlavorRef(description.instanceType)
    }

    // disks
    List<Disk> disks = []
    if (description.systemDisk) {
      def systemDisk = new Disk()
      systemDisk.setDiskType(Disk.DiskTypeEnum.SYS)
      systemDisk.setSize(description.systemDisk.diskSize as Integer)
      systemDisk.setVolumeType(Disk.VolumeTypeEnum.fromValue(description.systemDisk.diskType))
      disks.add(systemDisk)
    }
    if (description.dataDisks) {
      def dataDisks = description.dataDisks.collect {
        def dataDisk = new Disk()
        dataDisk.setDiskType(Disk.DiskTypeEnum.DATA)
        dataDisk.setSize(it.diskSize as Integer)
        dataDisk.setVolumeType(Disk.VolumeTypeEnum.fromValue(it.diskType))
        dataDisk
      }
      disks.addAll(dataDisks)
    }
    if (!disks.isEmpty()) {
      instanceConfig.setDisk(disks)
    }

    // public ip
    if (description.internetAccessible.publicIpAssigned) {
      def publicIp = new PublicIp()
      def eip = new Eip()
      def bandwidth = new Bandwidth()
      bandwidth.setSize(description.internetAccessible.internetMaxBandwidthOut as Integer)
      bandwidth.setShareType(Bandwidth.ShareTypeEnum.PER)
      // charging mode 
      def charge = description.internetAccessible.internetChargeType
      if (charge == "TRAFFIC_POSTPAID_BY_HOUR") {
        bandwidth.setChargingMode(Bandwidth.ChargingModeEnum.TRAFFIC)
      } else {
        bandwidth.setChargingMode(Bandwidth.ChargingModeEnum.BANDWIDTH)
      }

      eip.setIpType(Eip.IpTypeEnum._5_BGP)
      eip.setBandwidth(bandwidth)
      publicIp.setEip(eip)
      instanceConfig.setPublicIp(publicIp)
    }

    // keypair
    if (description.keyPair) {
      instanceConfig.setKeyName(description.keyPair)
    }

    // security groups
    if (description.securityGroupIds) {
      List<SecurityGroups> allSecurityGroups = []
      def securityGroup = new SecurityGroups()
      securityGroup.setId(description.securityGroupIds)
      allSecurityGroups.add(securityGroup)
      instanceConfig.setSecurityGroups(allSecurityGroups)
    }

    // user data
    if (description.userData) {
      instanceConfig.setUserData(description.userData)
    }

    body.setInstanceConfig(instanceConfig)
    request.setBody(body)
    request
  }

  private static def buildAutoScalingGroupRequest(HuaweiCloudDeployDescription description, String launchConfigurationId) {
    def request = new CreateScalingGroupRequest()
    def body = new CreateScalingGroupRequestBody()
    body.setScalingGroupName(description.serverGroupName)
    body.setScalingConfigurationId(launchConfigurationId)
    body.setDesireInstanceNumber(description.desiredCapacity)
    body.setMinInstanceNumber(description.minSize)
    body.setMaxInstanceNumber(description.maxSize)
    body.setVpcId(description.vpcId)

    // set default delete option
    body.setDeletePublicip(true)

    // networks
    if (description.subnetIds) {
      List<Networks> networks = []
      description.subnetIds.each {
        def network = new Networks().withId(it)
        networks.add(network)
      }
      body.setNetworks(networks)
    }

    // cooldown time
    if (description.defaultCooldown) {
      body.setCoolDownTime(description.defaultCooldown)
    }

    // zones
    if (description.zones.size() > 0) {
      body.setAvailableZones(description.zones)
    }

    // loadbalancer
    if (description.forwardLoadBalancers) {
      def forwardLoadBalancers = description.forwardLoadBalancers.collect {
        def forwardLoadBalancer = new LbaasListeners()
        forwardLoadBalancer.setPoolId(it.poolId)
        if (it.targetAttributes.size() > 0) {
          def target = it.targetAttributes[0]
          forwardLoadBalancer.setProtocolPort(target.port as Integer)
          forwardLoadBalancer.setWeight(target.weight as Integer)
        }
        forwardLoadBalancer
      }
      body.setLbaasListeners(forwardLoadBalancers)
    }

    // instance terminate policy
    if (description.terminationPolicies) {
      def policy = CreateScalingGroupRequestBody.InstanceTerminatePolicyEnum.fromValue(description.terminationPolicies[0])
      body.setInstanceTerminatePolicy(policy)
    }

    // instance health check
    if (description.healthAuditMethod) {
      def auditMethod = CreateScalingGroupRequestBody.HealthPeriodicAuditMethodEnum.fromValue(description.healthAuditMethod)
      body.setHealthPeriodicAuditMethod(auditMethod)
    }
    if (description.healthPeriodicTime) {
      def periodicTime = CreateScalingGroupRequestBody.HealthPeriodicAuditTimeEnum.fromValue(description.healthPeriodicTime)
      body.setHealthPeriodicAuditTime(periodicTime)
    }
    if (description.healthGracePeriod) {
      body.setHealthPeriodicAuditGracePeriod(description.healthGracePeriod)
    }

    request.setBody(body)
    request
  }

  List<ScalingGroups> getAllAutoScalingGroups() {
    def startNumber = 0
    List<ScalingGroups> scalingGroupAll = []
    try {
      while(true) {
        def req = new ListScalingGroupsRequest().withLimit(DEFAULT_LIMIT).withStartNumber(startNumber)
        def resp = client.listScalingGroups(req)
        if(resp == null || resp.getScalingGroups() == null || resp.getScalingGroups().size() == 0) {
          break
        }
        scalingGroupAll.addAll(resp.getScalingGroups())
        startNumber += DEFAULT_LIMIT
      }
      return scalingGroupAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<ScalingGroups> getAutoScalingGroupsByName(String name) {
    try {
      def req = new ListScalingGroupsRequest().withLimit(DEFAULT_LIMIT)
      req.setScalingGroupName(name)
      def resp = client.listScalingGroups(req)
      resp.getScalingGroups()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<ScalingConfiguration> getLaunchConfigurations() {
    def startNumber = 0
    List<ScalingConfiguration> scalingConfigAll = []
    try {
      while(true) {
        def req = new ListScalingConfigsRequest().withLimit(DEFAULT_LIMIT).withStartNumber(startNumber)
        def resp = client.listScalingConfigs(req)
        if(resp == null || resp.getScalingConfigurations() == null || resp.getScalingConfigurations().size() == 0) {
          break
        }
        scalingConfigAll.addAll(resp.getScalingConfigurations())
        startNumber += DEFAULT_LIMIT
      }
      return scalingConfigAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<ScalingGroupInstance> getAutoScalingInstances(String asgId=null) {
    try {
      def req = new ListScalingInstancesRequest()
      if (asgId) {
        req.setScalingGroupId(asgId)
      }
      def resp = client.listScalingInstances req
      resp.getScalingGroupInstances()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def getAutoScalingActivitiesByAsgId(String asgId, maxActivityNum=100) {
    try {
      def req = new ListScalingActivityLogsRequest()
      req.setScalingGroupId(asgId)
      req.setLimit(maxActivityNum)
      def resp = client.listScalingActivityLogs req
      resp.getScalingActivityLog()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def getAutoScalingTags(String asgId) {
    try {
      def req = new ListScalingTagInfosByResourceIdRequest()
      req.setResourceType(ListScalingTagInfosByResourceIdRequest.ResourceTypeEnum.SCALING_GROUP_TAG)
      req.setResourceId(asgId)
      def resp = client.listScalingTagInfosByResourceId req
      resp.getTags()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<ScalingPolicyDetail> getScalingPolicies(String asgId=null) {
    try {
      def req = new ListScalingPoliciesRequest()
      if (asgId) {
        req.setScalingGroupId(asgId)
      }
      def resp = client.listScalingPolicies req
      resp.getScalingPolicies()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def createScalingPolicy(String asgId, String asgName, ScalingPolicyDetail policy, String alarmId) {
    try {
      def request = new CreateScalingPolicyRequest()
      def body = new CreateScalingPolicyRequestBody()
      body.setScalingGroupId(asgId)
      // policy name
      def scalingPolicyName = asgName + "-asp-" + new Date().time.toString()
      body.setScalingPolicyName(scalingPolicyName)
      // policy type
      def scalingPolicyType = policy.getScalingPolicyType().toString()
      body.setScalingPolicyType(CreateScalingPolicyRequestBody.ScalingPolicyTypeEnum.fromValue(scalingPolicyType))
      if (scalingPolicyType == "ALARM" && alarmId) {
        body.setAlarmId(alarmId)
      } else {
        def scheduledPolicy = policy.getScheduledPolicy()
        def newScheduledPolicy = new ScheduledPolicy()
        newScheduledPolicy.setLaunchTime(scheduledPolicy.getLaunchTime())
        if (scalingPolicyType == "RECURRENCE") {
          if (scheduledPolicy.getRecurrenceType()) {
            newScheduledPolicy.setRecurrenceType(scheduledPolicy.getRecurrenceType())
          }
          if (scheduledPolicy.getRecurrenceValue()) {
            newScheduledPolicy.setRecurrenceValue(scheduledPolicy.getRecurrenceValue())
          }
          if (scheduledPolicy.getStartTime()) {
            newScheduledPolicy.setStartTime(scheduledPolicy.getStartTime())
          }
          if (scheduledPolicy.getEndTime()) {
            newScheduledPolicy.setEndTime(scheduledPolicy.getEndTime())
          }
        }
        body.setScheduledPolicy(newScheduledPolicy)
      }
      // policy action
      def policyAction = policy.getScalingPolicyAction()
      def newPolicyAction = new ScalingPolicyAction()
      if (policyAction.getOperation()) {
        newPolicyAction.setOperation(policyAction.getOperation())
      }
      if (policyAction.getInstanceNumber()) {
        newPolicyAction.setInstanceNumber(policyAction.getInstanceNumber())
      }
      if (policyAction.getInstancePercentage()) {
        newPolicyAction.setInstancePercentage(policyAction.getInstancePercentage())
      }
      body.setScalingPolicyAction(newPolicyAction)

      // cool down time
      body.setCoolDownTime(policy.getCoolDownTime())
      request.setBody(body)
      def response = client.createScalingPolicy request
      response
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  List<Topics> getNotification(String asgId=null) {
    try {
      def req = new ListScalingNotificationsRequest()
      if (asgId) {
        req.setScalingGroupId(asgId)
      }
      def resp = client.listScalingNotifications req
      resp.getTopics()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def createNotification(String asgId, Topics notification) {
    try {
      def request = new CreateScalingNotificationRequest()
      def body = new CreateNotificationRequestBody()
      request.setScalingGroupId(asgId)
      body.setTopicUrn(notification.getTopicUrn())
      body.setTopicScene(notification.getTopicScene())
      request.setBody(body)
      def response = client.createScalingNotification request
      response
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  List<LifecycleHookList> getLifeCycleHook(String asgId=null) {
    try {
      def req = new ListLifeCycleHooksRequest()
      if (asgId) {
        req.setScalingGroupId(asgId)
      }
      def resp = client.listLifeCycleHooks req
      resp.getLifecycleHooks()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  def createLifeCycleHook(String asgId, LifecycleHookList hook) {
    try {
      def request = new CreateLifyCycleHookRequest()
      def body = new CreateLifeCycleHookRequestBody()
      request.setScalingGroupId(asgId)
      body.setLifecycleHookName(hook.getLifecycleHookName())
      // Lifecycle Hook Type
      def hookType = hook.getLifecycleHookType().toString()
      body.setLifecycleHookType(CreateLifeCycleHookRequestBody.LifecycleHookTypeEnum.fromValue(hookType))
      // Lifecycle Default Result
      def defaultResult = hook.getDefaultResult().toString()
      if (defaultResult) {
        body.setDefaultResult(CreateLifeCycleHookRequestBody.DefaultResultEnum.fromValue(defaultResult))
      }
      // Default Timeout
      if (hook.getDefaultTimeout()) {
        body.setDefaultTimeout(hook.getDefaultTimeout())
      }
      // Topic Urn
      body.setNotificationTopicUrn(hook.getNotificationTopicUrn())
      // Metadata
      if (hook.getNotificationMetadata()) {
        body.setNotificationMetadata(hook.getNotificationMetadata())
      }
      request.setBody(body)
      def response = client.createLifyCycleHook request
      response
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  void resizeAutoScalingGroup(String asgId, def capacity) {
    try {
      def request = new UpdateScalingGroupRequest()
      def body = new UpdateScalingGroupRequestBody()
      request.setScalingGroupId(asgId)
      body.setMaxInstanceNumber(capacity.max)
      body.setMinInstanceNumber(capacity.min)
      body.setDesireInstanceNumber(capacity.desired)
      request.setBody(body)

      client.updateScalingGroup(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  void enableAutoScalingGroup(String asgId) {
    try {
      def request = new EnableOrDisableScalingGroupRequest()
      def body = new EnableOrDisableScalingGroupRequestBody()
      body.setAction(EnableOrDisableScalingGroupRequestBody.ActionEnum.RESUME)
      request.setScalingGroupId(asgId)
      request.setBody(body)
      client.enableOrDisableScalingGroup(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  void disableAutoScalingGroup(String asgId) {
    try {
      def request = new EnableOrDisableScalingGroupRequest()
      def body = new EnableOrDisableScalingGroupRequestBody()
      body.setAction(EnableOrDisableScalingGroupRequestBody.ActionEnum.PAUSE)
      request.setScalingGroupId(asgId)
      request.setBody(body)
      client.enableOrDisableScalingGroup(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  String deleteAutoScalingGroup(String asgId) {
    try {
      def request = new DeleteScalingGroupRequest()
      request.setScalingGroupId(asgId)
      request.setForceDelete(DeleteScalingGroupRequest.ForceDeleteEnum.fromValue("yes"))
      client.deleteScalingGroup(request)

      // wait for deleting successed
      for (def i = 0; i < MAX_TRY_COUNT; i++) {
        Thread.sleep(REQ_TRY_INTERVAL)
        def getReq = new ShowScalingGroupRequest()
        getReq.setScalingGroupId(asgId)
        client.showScalingGroup(getReq)
      }
    } catch (ServiceResponseException e) {
      if (e.getHttpStatusCode() == 404) {
        return "success"
      }
      throw new HuaweiCloudOperationException(e.toString())
    }
    return ""
  }

  void deleteLaunchConfiguration(String ascId) {
    try {
      def request = new DeleteScalingConfigRequest()
      request.setScalingConfigurationId(ascId)
      client.deleteScalingConfig(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  void removeInstances(def asgId, def instanceIds) {
    try {
      def request = new DeleteScalingInstanceRequest()
      request.setInstanceId(instanceIds[0])
      request.setInstanceDelete(DeleteScalingInstanceRequest.InstanceDeleteEnum.YES)
      client.deleteScalingInstance(request)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

}
