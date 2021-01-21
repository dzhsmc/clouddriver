package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.ResizeHuaweiCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HuaweiCloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeHuaweiCloudServerGroupDescription")
class ResizeHuaweiCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new ResizeHuaweiCloudServerGroupAtomicOperation(convertDescription(input))
  }

  ResizeHuaweiCloudServerGroupDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, ResizeHuaweiCloudServerGroupDescription)
  }
}
