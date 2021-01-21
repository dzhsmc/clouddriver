package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateAndDecrementHuaweiCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors


@HuaweiCloudOperation(AtomicOperations.TERMINATE_INSTANCE_AND_DECREMENT)
@Component("terminateAndDecrementHuaweiCloudServerGroupDescriptionValidator")
class TerminateAndDecrementHuaweiCloudServerGroupDescriptionValidator extends DescriptionValidator<TerminateAndDecrementHuaweiCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, TerminateAndDecrementHuaweiCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateAndDecrementHuaweiCloudServerGroupDescription.region.empty"
    }

    if (!description.instance) {
      errors.rejectValue "instance", "TerminateAndDecrementHuaweiCloudServerGroupDescription.instance.empty"
    }
  }
}
