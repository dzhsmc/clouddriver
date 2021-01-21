package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.DisableHuaweiCloudServerGroupAtomicOperation
import org.springframework.stereotype.Component

@HuaweiCloudOperation(AtomicOperations.DISABLE_SERVER_GROUP)
@Component("disableHuaweiCloudServerGroupDescription")
class DisableHuaweiCloudServerGroupAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport {

  AtomicOperation convertOperation(Map input) {
    new DisableHuaweiCloudServerGroupAtomicOperation(convertDescription(input))
  }

  EnableDisableHuaweiCloudServerGroupDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, EnableDisableHuaweiCloudServerGroupDescription)
  }
}
