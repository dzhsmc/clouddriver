package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeleteHuaweiCloudLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.DeleteHuaweiCloudLoadBalancerAtomicOperation
import org.springframework.stereotype.Component


@HuaweiCloudOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteHuaweiCloudLoadBalancerDescription")
class DeleteHuaweiCloudLoadBalancerAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new DeleteHuaweiCloudLoadBalancerAtomicOperation(convertDescription(input))
  }

  DeleteHuaweiCloudLoadBalancerDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, DeleteHuaweiCloudLoadBalancerDescription)
  }
}
