package com.netflix.spinnaker.clouddriver.alicloud.deploy.model;

import lombok.Data;

@Data
public class Source {
  private String account;
  private String region;
  private String asgName;
  private Boolean useSourceCapacity;
}
