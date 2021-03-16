package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DestroyHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudInstance
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DestroyHuaweiCloudServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "DESTROY_SERVER_GROUP"

  DestroyHuaweiCloudServerGroupDescription description

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  DestroyHuaweiCloudServerGroupAtomicOperation(DestroyHuaweiCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    // 1. force delete asg
    // 2. delete asc
    task.updateStatus BASE_PHASE, "Initializing destroy server group $description.serverGroupName in " +
      "$description.region..."
    def region = description.region
    def accountName = description.accountName
    def serverGroupName = description.serverGroupName

    def client = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )

    task.updateStatus(BASE_PHASE, "Start destroy server group $serverGroupName")
    def serverGroup = huaweicloudClusterProvider.getServerGroup(accountName, region, serverGroupName, true)

    if (serverGroup) {
      String asgId = serverGroup.asg.autoScalingGroupId
      String ascId = serverGroup.asg.launchConfigurationId

      task.updateStatus(BASE_PHASE, "Server group $serverGroupName is related to " +
        "auto scaling group $asgId and launch configuration $ascId.")

      task.updateStatus(BASE_PHASE, "Force deleting auto scaling group $asgId...")
      def ret = client.deleteAutoScalingGroup(asgId)
      if (ret == "success") {
        task.updateStatus(BASE_PHASE, "Auto scaling group $asgId is deleted.")
      } else {
        task.updateStatus(BASE_PHASE, "Auto scaling group $asgId force deleting timeout.")
        return ""
      }

      task.updateStatus(BASE_PHASE, "Deleting launch configuration $ascId...")
      client.deleteLaunchConfiguration(ascId)
      task.updateStatus(BASE_PHASE, "Launch configuration $ascId is deleted.")

      task.updateStatus(BASE_PHASE, "Complete destroy server group $serverGroupName.")
    } else {
      task.updateStatus(BASE_PHASE, "Server group $serverGroupName is not found.")
    }

    task.updateStatus(BASE_PHASE, "Complete destroy server group. ")
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
