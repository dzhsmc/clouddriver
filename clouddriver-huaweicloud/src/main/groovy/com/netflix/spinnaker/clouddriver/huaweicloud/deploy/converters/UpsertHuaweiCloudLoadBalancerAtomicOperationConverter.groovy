package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.converters

import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudOperation
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.UpsertHuaweiCloudLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.UpsertHuaweiCloudLoadBalancerAtomicOperation
import org.springframework.stereotype.Component


@HuaweiCloudOperation(AtomicOperations.UPSERT_LOAD_BALANCER)
@Component("upsertHuaweiCloudLoadBalancerDescription")
class UpsertHuaweiCloudLoadBalancerAtomicOperationConverter extends AbstractAtomicOperationsCredentialsSupport{
  AtomicOperation convertOperation(Map input) {
    new UpsertHuaweiCloudLoadBalancerAtomicOperation(convertDescription(input))
  }

  UpsertHuaweiCloudLoadBalancerDescription convertDescription(Map input) {
    HuaweiCloudAtomicOperationConverterHelper.convertDescription(input, this, UpsertHuaweiCloudLoadBalancerDescription)
  }
}
