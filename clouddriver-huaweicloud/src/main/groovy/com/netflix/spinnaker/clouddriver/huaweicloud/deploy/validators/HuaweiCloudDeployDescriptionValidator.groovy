package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.HuaweiCloudDeployDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.CREATE_SERVER_GROUP)
@Component("huaweicloudDeployDescriptionValidator")
class HuaweiCloudDeployDescriptionValidator extends DescriptionValidator<HuaweiCloudDeployDescription> {
  @Override
  void validate(List priorDescriptions, HuaweiCloudDeployDescription description, Errors errors) {

    if (!description.application) {
      errors.rejectValue "application", "huaweicloudDeployDescription.application.empty"
    }

    if (!description.imageId) {
      errors.rejectValue "imageId", "huaweicloudDeployDescription.imageId.empty"
    }

    if (!description.instanceType) {
      errors.rejectValue "instanceType", "huaweicloudDeployDescription.instanceType.empty"
    }

    if (!description.subnetIds) {
      errors.rejectValue "subnetIds", "huaweicloudDeployDescription.subnetIds.empty"
    }

    if (description.maxSize == null) {
      errors.rejectValue "maxSize", "huaweicloudDeployDescription.maxSize.empty"
    }

    if (description.minSize == null) {
      errors.rejectValue "minSize", "huaweicloudDeployDescription.minSize.empty"
    }

    if (description.desiredCapacity == null) {
      errors.rejectValue "desiredCapacity", "huaweicloudDeployDescription.desiredCapacity.empty"
    }

  }
}
