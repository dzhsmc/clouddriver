package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateHuaweiCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class TerminateHuaweiCloudInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "TERMINATE_INSTANCES"

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  TerminateHuaweiCloudInstancesDescription description

  TerminateHuaweiCloudInstancesAtomicOperation(TerminateHuaweiCloudInstancesDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = description.instanceIds
    def accountName = description.accountName

    task.updateStatus BASE_PHASE, "Initializing termination of instances (${description.instanceIds.join(", ")}) in " +
      "$description.region:$serverGroupName..."

    def client = new HuaweiElasticCloudServerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )
    client.terminateInstances(instanceIds)

    task.updateStatus BASE_PHASE, "Complete termination of instance."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
