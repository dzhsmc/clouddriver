package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiElasticCloudServerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.RebootHuaweiCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudClusterProvider
import org.springframework.beans.factory.annotation.Autowired

class RebootHuaweiCloudInstancesAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "REBOOT_INSTANCES"

  RebootHuaweiCloudInstancesDescription description

  @Autowired
  HuaweiCloudClusterProvider huaweicloudClusterProvider

  RebootHuaweiCloudInstancesAtomicOperation(RebootHuaweiCloudInstancesDescription description) {
    this.description = description
  }

  @Override
  Void operate(List priorOutputs) {
    def region = description.region
    def serverGroupName = description.serverGroupName
    def instanceIds = description.instanceIds

    task.updateStatus BASE_PHASE, "Initializing reboot of instances (${description.instanceIds.join(", ")}) in " +
      "$description.region:$serverGroupName..."

    def client = new HuaweiElasticCloudServerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      region
    )
    client.rebootInstances(instanceIds)
    task.updateStatus BASE_PHASE, "Complete reboot of instance."
    return null
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
