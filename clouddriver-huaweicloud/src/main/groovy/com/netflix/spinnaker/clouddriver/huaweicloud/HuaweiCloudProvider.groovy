package com.netflix.spinnaker.clouddriver.huaweicloud

import com.netflix.spinnaker.clouddriver.core.CloudProvider
import org.springframework.stereotype.Component

import java.lang.annotation.Annotation

@Component
class HuaweiCloudProvider implements CloudProvider {
  public static final String ID = "huaweicloud"
  final String id = ID
  final String displayName = "HuaweiCloud"
  final Class<Annotation> operationAnnotationType = HuaweiCloudOperation
}
