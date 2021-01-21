package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.validators

import com.netflix.spinnaker.clouddriver.deploy.DescriptionValidator
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.UpsertHuaweiCloudLoadBalancerDescription
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component
import org.springframework.validation.Errors

@Slf4j
@HuaweiCloudOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component("upsertHuaweiCloudLoadBalancerDescriptionValidator")
class UpsertHuaweiCloudLoadBalancerDescriptionValidator extends DescriptionValidator<UpsertHuaweiCloudLoadBalancerDescription> {
  @Override
  void validate(List priorDescriptions, UpsertHuaweiCloudLoadBalancerDescription description, Errors errors) {
    log.info("Enter huaweicloud validate ${description.properties}")
    if (!description.application) {
      errors.rejectValue "application", "UpsertHuaweiCloudLoadBalancerDescription.application.empty"
    }

    if (!description.accountName) {
      errors.rejectValue "accountName", "UpsertHuaweiCloudLoadBalancerDescription.accountName.empty"
    }

    if (!description.loadBalancerName) {
      errors.rejectValue "loadBalancerName", "UpsertHuaweiCloudLoadBalancerDescription.loadBalancerName.empty"
    }

    if (!description.region) {
      errors.rejectValue "region", "UpsertHuaweiCloudLoadBalancerDescription.region.empty"
    }

    //listener check

    //rule check
  }
}
