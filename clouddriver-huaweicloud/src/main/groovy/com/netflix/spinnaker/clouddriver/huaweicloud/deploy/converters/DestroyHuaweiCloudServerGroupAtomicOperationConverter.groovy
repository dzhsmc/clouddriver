package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DestroyHuaweiCloudServerGroupDescription
import org.springframework.stereotype.Component
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.DestroyHuaweiCloudServerGroupAtomicOperation

@HuaweiCloudOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyHuaweiCloudServerGroupDescription")
class DestroyHuaweiCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new DestroyHuaweiCloudServerGroupAtomicOperation(convertDescription(input))
  }

  DestroyHuaweiCloudServerGroupDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, DestroyHuaweiCloudServerGroupDescription)
  }
}
