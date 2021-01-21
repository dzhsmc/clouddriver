package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.DeleteHuaweiCloudLoadBalancerDescription
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@HuaweiCloudOperation(AtomicOperations.DELETE_LOAD_BALANCER)
@Component("deleteHuaweiCloudLoadBalancerDescriptionValidator")
class DeleteHuaweiCloudLoadBalancerDescriptionValidator extends DescriptionValidator<DeleteHuaweiCloudLoadBalancerDescription> {
  @Override
  void validate(List priorDescriptions, DeleteHuaweiCloudLoadBalancerDescription description, Errors errors) {

    if (!description.application) {
      errors.rejectValue "application", "DeleteHuaweiCloudLoadBalancerDescription.application.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "DeleteHuaweiCloudLoadBalancerDescription.accountName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "UpsertHuaweiCloudLoadBalancerDescription.region.empty"
    }

    if (!description.loadBalancerId) {
      errors.rejectValue "loadBalancerId", "DeleteHuaweiCloudLoadBalancerDescription.loadBalancerId.empty"
    }
  }
}
