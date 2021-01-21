package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.client.LoadBalancerClient
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeleteHuaweiCloudLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerRule
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.huaweicloud.provider.view.HuaweiCloudLoadBalancerProvider
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteLoadBalancer": {"application":"myapplication", "account":"account-test","loadBalancerId": "lb-kf2lp6cj", "region":"ap-guangzhou", "listener":[{"listenerId":"lbl-d2no6v2c", "targets":[{"instanceId":"ins-lq6o6xyc","port":8080}]}] }} ]' localhost:7004/huaweicloud/ops
 *
 * curl -X POST -H "Content-Type: application/json" -d '[ { "deleteLoadBalancer": {"application":"myapplication", "account":"account-test","loadBalancerId": "lb-kf2lp6cj", "region":"ap-guangzhou", "listener":[{"listenerId":"lbl-hzrdz86n","rules":[{"locationId":"loc-lbcmvnlt","targets":[{"instanceId":"ins-lq6o6xyc","port":8080}]}]}] }} ]' localhost:7004/huaweicloud/ops
 */

@Slf4j
class DeleteHuaweiCloudLoadBalancerAtomicOperation implements AtomicOperation<Void> {

  private static final String BASE_PHASE = "DELETE_LOAD_BALANCER"
  DeleteHuaweiCloudLoadBalancerDescription description

  @Autowired
  HuaweiCloudLoadBalancerProvider huaweicloudLoadBalancerProvider

  DeleteHuaweiCloudLoadBalancerAtomicOperation(DeleteHuaweiCloudLoadBalancerDescription description) {
    this.description = description
  }

  @Override
   Void operate(List priorOutputs) {
    task.updateStatus(BASE_PHASE, "Initializing delete of HuaweiCloud loadBalancer ${description.loadBalancerId} " +
      "in ${description.region}...")
    log.info("params = ${description}")

    def lbListener = description.listener
    if (lbListener?.size() > 0) {    //listener
      lbListener.each {
        def listenerId = it.listenerId
        def rules = it.rules
        def targets = it.targets
        if (rules?.size() > 0) {
          rules.each {
            def ruleTargets = it.targets
            if (ruleTargets?.size() > 0) {    //delete rule's targets
              deleteRuleTargets(it.poolId, ruleTargets)
            }else {  //delete rule
              deleteListenerRule(it)
            }
          }
        }else if (targets?.size() > 0) {    //delete listener's targets
          deleteListenerTargets(it.poolId, targets)
        }else {    //delete listener
          deleteListener(listenerId)
        }
      }
    }else {    //no listener, delete loadBalancer
      deleteLoadBalancer(description.loadBalancerId)
    }
    return null
  }

  private void deleteLoadBalancer(String loadBalancerId) {
    task.updateStatus(BASE_PHASE, "Start delete loadBalancer ${loadBalancerId} ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      description.region
    )
    def ret = lbClient.deleteLoadBalancerById(loadBalancerId)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${loadBalancerId} ${ret} end")
  }

  private void deleteListener(String listenerId) {
    task.updateStatus(BASE_PHASE, "Start delete Listener ${listenerId} ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      description.region
    )
    def ret = lbClient.deleteLBListenerById(listenerId)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer ${listenerId} ${ret} end")
  }

  private void deleteListenerTargets(String poolId, List<HuaweiCloudLoadBalancerTarget> targets) {
    task.updateStatus(BASE_PHASE, "Start delete loadBalancer Pool ${poolId} targets ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      description.region
    )
    def ret = lbClient.deRegisterTarget(poolId, targets)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer Pool ${poolId} targets ${ret} end")
  }

  private void deleteListenerRule(HuaweiCloudLoadBalancerRule rule) {
    task.updateStatus(BASE_PHASE, "Start delete Listener rules ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      description.region
    )
    def ret = lbClient.deleteLBListenerRule(rule)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer rules ${ret} end")
  }

  private void deleteRuleTargets(String poolId, List<HuaweiCloudLoadBalancerTarget> targets) {
    task.updateStatus(BASE_PHASE, "Start delete loadBalancer Pool ${poolId} rule  targets ...")
    def lbClient = new LoadBalancerClient(
      description.credentials.credentials.accessKeyId,
      description.credentials.credentials.accessSecretKey,
      description.region
    )
    def ret = lbClient.deRegisterTarget(poolId, targets)
    task.updateStatus(BASE_PHASE, "Delete loadBalancer Pool ${poolId} rule targets ${ret} end")
  }

  private static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }
}
