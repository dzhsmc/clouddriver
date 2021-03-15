package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.ENABLE_SERVER_GROUP)
@Component("enableHuaweiCloudServerGroupDescriptionValidator")
class EnableHuaweiCloudServerGroupDescriptionValidator extends DescriptionValidator<EnableDisableHuaweiCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, EnableDisableHuaweiCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "enableHuaweiCloudServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "enableHuaweiCloudServerGroupDescription.serverGroupName.empty"
    }
  }
}
