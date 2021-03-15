package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateHuaweiCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.TerminateHuaweiCloudInstancesAtomicOperation
import org.springframework.stereotype.Component


@HuaweiCloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateHuaweiCloudInstancesDescription")
class TerminateHuaweiCloudInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  TerminateHuaweiCloudInstancesDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, TerminateHuaweiCloudInstancesDescription)
  }

  @Override
  AtomicOperation convertOperation(Map input) {
    new TerminateHuaweiCloudInstancesAtomicOperation(convertDescription(input))
  }
}
