package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops


import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription
import groovy.transform.Canonical

abstract class AbstractEnableDisableAtomicOperation implements AtomicOperation<Void> {
  EnableDisableHuaweiCloudServerGroupDescription description
  abstract boolean isDisable()
  abstract String getBasePhase()

  AbstractEnableDisableAtomicOperation(EnableDisableHuaweiCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    String basePhase = getBasePhase()

    task.updateStatus basePhase,"Initializing disable server group $description.serverGroupName in $description.region..."

    def serverGroupName = description.serverGroupName
    def region = description.region
    def client = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )

    def ecsClient = new HuaweiElasticCloudServerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )

    // find auto scaling group
    def asg = getAutoScalingGroup client, serverGroupName
    def asgId = asg.getScalingGroupId()

    // enable or disable auto scaling group
    enableOrDisableAutoScalingGroup client, asgId

    // get in service instances in auto scaling group
    if (isDisable()) {
      def inServiceInstanceIds = getInServiceAutoScalingInstances client, asgId
      if (!inServiceInstanceIds) {
        task.updateStatus basePhase, "Auto scaling group has no IN_SERVICE instance. "
        return null
      }
      if (inServiceInstanceIds.size() > 0) {
        ecsClient.terminateInstances(inServiceInstanceIds)
      }
    }

    task.updateStatus basePhase, "Complete enable server group $serverGroupName in $region."
    null
  }

  private def getAutoScalingGroup(HuaweiAutoScalingClient client, String serverGroupName) {
    def asgs = client.getAutoScalingGroupsByName serverGroupName
    if (asgs) {
      def asg = asgs[0]
      def asgId = asg.getScalingGroupId()
      task.updateStatus basePhase,"Server group $serverGroupName's auto scaling group id is $asgId"
      asg
    } else {
      task.updateStatus basePhase,"Server group $serverGroupName is not found."
      null
    }
  }

  private void enableOrDisableAutoScalingGroup(HuaweiAutoScalingClient client, String asgId) {
    if (isDisable()) {
      task.updateStatus basePhase, "Disabling auto scaling group $asgId..."
      client.disableAutoScalingGroup asgId
      task.updateStatus basePhase, "Auto scaling group $asgId status is disabled."
    } else {
      task.updateStatus basePhase, "Enabling auto scaling group $asgId..."
      client.enableAutoScalingGroup asgId
      task.updateStatus basePhase, "Auto scaling group $asgId status is enabled."
    }
  }

  private def getInServiceAutoScalingInstances(HuaweiAutoScalingClient client, String asgId) {
    task.updateStatus basePhase, "Get instances managed by auto scaling group $asgId"
    def instances = client.getAutoScalingInstances asgId

    if (!instances) {
      task.updateStatus basePhase, "Found no instance in $asgId."
      return null
    }

    def inServiceInstanceIds = instances.collect {
      // if (it.getHealthStatus().toString() == 'NORMAL' && it.getLifeCycleState().toString() == 'INSERVICE') {
      //   it.getInstanceId()
      // } else {
      //   null
      // }
      it.getInstanceId()
    } as List<String>

    task.updateStatus basePhase, "Auto scaling group $asgId has InService instances $inServiceInstanceIds"
    inServiceInstanceIds
  }

  static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  @Canonical
  static class Target {
    String instanceId
    Integer weight
    Integer port
  }
}
