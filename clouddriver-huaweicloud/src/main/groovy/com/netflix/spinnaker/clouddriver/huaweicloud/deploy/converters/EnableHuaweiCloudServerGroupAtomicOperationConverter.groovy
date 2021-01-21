package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.EnableHuaweiCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HuaweiCloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableHuaweiCloudServerGroupDescription")
class EnableHuaweiCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {
  AtomicOperation convertOperation(Map input) {
    new EnableHuaweiCloudServerGroupAtomicOperation(convertDescription(input))
  }

  EnableDisableHuaweiCloudServerGroupDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, EnableDisableHuaweiCloudServerGroupDescription)
  }
}
