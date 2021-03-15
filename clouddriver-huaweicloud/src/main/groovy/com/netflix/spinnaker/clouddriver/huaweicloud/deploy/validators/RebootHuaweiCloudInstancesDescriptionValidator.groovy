package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.RebootHuaweiCloudInstancesDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.REBOOT_INSTANCES)
@Component("rebootHuaweiCloudInstancesDescriptionValidator")
class RebootHuaweiCloudInstancesDescriptionValidator extends DescriptionValidator<RebootHuaweiCloudInstancesDescription> {
  @Override
  void validate(List priorDescriptions, RebootHuaweiCloudInstancesDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "RebootHuaweiCloudInstancesDescription.region.empty"
    }

    if (!description.instanceIds) {
      errors.rejectValue "instanceIds", "RebootHuaweiCloudInstancesDescription.instanceIds.empty"
    }
  }
}
