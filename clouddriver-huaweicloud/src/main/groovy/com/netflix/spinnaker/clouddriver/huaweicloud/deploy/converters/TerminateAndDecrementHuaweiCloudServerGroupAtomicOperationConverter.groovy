package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateAndDecrementHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.TerminateAndDecrementHuaweiCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HuaweiCloudOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementHuaweiCloudServerGroupDescription")
class TerminateAndDecrementHuaweiCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateAndDecrementHuaweiCloudServerGroupDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, TerminateAndDecrementHuaweiCloudServerGroupDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateAndDecrementHuaweiCloudServerGroupAtomicOperation(convertDescription(input))
  }
}
