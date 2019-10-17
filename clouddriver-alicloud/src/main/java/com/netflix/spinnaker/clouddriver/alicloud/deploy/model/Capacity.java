package com.netflix.spinnaker.clouddriver.alicloud.deploy.model;

import lombok.Data;

@Data
public class Capacity {
  private Integer min;
  private Integer max;
  private Integer desired;
}
