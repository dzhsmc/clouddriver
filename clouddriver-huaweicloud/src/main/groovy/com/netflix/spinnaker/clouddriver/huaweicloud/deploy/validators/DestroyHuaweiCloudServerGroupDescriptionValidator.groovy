package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DestroyHuaweiCloudServerGroupDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.DESTROY_SERVER_GROUP)
@Component("destroyHuaweiCloudServerGroupDescriptionValidator")
class DestroyHuaweiCloudServerGroupDescriptionValidator extends DescriptionValidator<DestroyHuaweiCloudServerGroupDescription> {
  @Override
  void validate(List priorDescriptions, DestroyHuaweiCloudServerGroupDescription description, Errors errors) {
    if (!description.region) {
      errors.rejectValue "region", "huaweicloudDestroyServerGroupDescription.region.empty"
    }

    if (!description.serverGroupName) {
      errors.rejectValue "serverGroupName", "huaweicloudDestroyServerGroupDescription.serverGroupName.empty"
    }
  }
}
