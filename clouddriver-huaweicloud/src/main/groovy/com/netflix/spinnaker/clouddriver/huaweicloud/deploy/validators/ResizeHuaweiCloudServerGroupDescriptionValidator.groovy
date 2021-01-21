package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.ResizeHuaweiCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.RESIZE_SERVER_GROUP)
@Component("resizeHuaweiCloudServerGroupDescriptionValidator")
class ResizeHuaweiCloudServerGroupDescriptionValidator extends DescriptionValidator<ResizeHuaweiCloudServerGroupDescription> {
  @Override
  void validate(
    List priorDescriptions, ResizeHuaweiCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "ResizeHuaweiCloudServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "ResizeHuaweiCloudServerGroupDescription.serverGroupName.empty"
    }

    if (description.capacity == null) {
      errors.rejectValue "capacity", "ResizeHuaweiCloudServerGroupDescription.capacity.empty"
    }
  }
}
