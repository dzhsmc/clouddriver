package com.netflix.spinnaker.clouddriver.alicloud.deploy.description;

import java.util.Map;
import lombok.Data;

@Data
public class UpsertAliCloudImageTagsDescription extends BaseAliCloudDescription {
  private String imageName;
  private Map<String, String> tags;
  private String accountName;
}
