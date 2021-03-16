package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiAutoScalingClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class ResizeHuaweiCloudServerGroupAtomicOperation implements AtomicOperation<Void> {
  private static final String BASE_PHASE = "RESIZE_SERVER_GROUP"

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  private final ResizeHuaweiCloudServerGroupDescription description

  ResizeHuaweiCloudServerGroupAtomicOperation(ResizeHuaweiCloudServerGroupDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    task.updateStatus BASE_PHASE, "Initializing resize of server group $description.serverGroupName in " +
      "$description.region..."
    def accountName = description.accountName
    //def credentials = description.credentials
    def region = description.region
    def serverGroupName = description.serverGroupName
    def asgId = huaweicloudClusterProvider.getServerGroupAsgId(serverGroupName, accountName, region)

    def client = new HuaweiAutoScalingClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )
    client.resizeAutoScalingGroup(asgId, description.capacity)
    task.updateStatus BASE_PHASE, "Complete resize of server group $description.serverGroupName in " +
      "$description.region."
    null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
