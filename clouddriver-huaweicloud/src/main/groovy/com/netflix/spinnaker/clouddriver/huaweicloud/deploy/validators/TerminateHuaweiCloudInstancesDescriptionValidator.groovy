package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.TerminateHuaweiCloudInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.TERMINATE_INSTANCES)
@Component("terminateHuaweiCloudInstancesDescriptionValidator")
class TerminateHuaweiCloudInstancesDescriptionValidator extends DescriptionValidator<TerminateHuaweiCloudInstancesDescription> {
  @Override
  void validate(List priorDescriptions, TerminateHuaweiCloudInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "TerminateHuaweiCloudInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "TerminateHuaweiCloudInstancesDescription.instanceIds.empty"
    }
  }
}
