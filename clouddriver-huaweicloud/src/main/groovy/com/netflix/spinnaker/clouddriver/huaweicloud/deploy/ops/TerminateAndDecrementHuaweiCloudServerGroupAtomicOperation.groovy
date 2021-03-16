package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateAndDecrementHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class TerminateAndDecrementHuaweiCloudServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "TERMINATE_AND_DEC_INSTANCES"

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  TerminateAndDecrementHuaweiCloudServerGroupDescription description

  TerminateAndDecrementHuaweiCloudServerGroupAtomicOperation(TerminateAndDecrementHuaweiCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = [description.instance]
    def accountName = description.credentials.name

    task.updateStatus BASE_PHASE, "Initializing termination of instance (${description.instance}) in " +
      "$description.region:$serverGroupName and decrease server group desired capacity..."

    def asgId = huaweicloudClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)
    def client = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )
    client.removeInstances(asgId, instanceIds)
    task.updateStatus BASE_PHASE, "Complete terminate instance and decrease server group desired capacity."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
