package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.RebootHuaweiCloudInstancesDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.RebootHuaweiCloudInstancesAtomicOperation
import org.springframework.stereotype.Component

@HuaweiCloudOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootHuaweiCloudInstancesDescription")
class RebootHuaweiCloudInstancesAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  @Override
  AtomicOperation convertOperation(Map input) {
    new RebootHuaweiCloudInstancesAtomicOperation(convertDescription(input))
  }

  @Override
  RebootHuaweiCloudInstancesDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, RebootHuaweiCloudInstancesDescription)
  }
}
