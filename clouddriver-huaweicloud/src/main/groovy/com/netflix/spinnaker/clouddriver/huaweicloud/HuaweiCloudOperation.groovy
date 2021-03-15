package com.netflix.spinnaker.clouddriver.huaweicloud

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface HuaweiCloudOperation {
  String value()
}
