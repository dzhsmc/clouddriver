package com.netflix.spinnaker.clouddriver.alicloud.deploy.ops;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeImagesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeImagesResponse;
import com.aliyuncs.ecs.model.v20140526.TagResourcesRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.UpsertAliCloudImageTagsDescription;
import com.netflix.spinnaker.clouddriver.alicloud.exception.AliCloudException;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import java.util.ArrayList;
import java.util.List;

public class UpsertAliCloudImageTagsAtomicOperation implements AtomicOperation<Void> {

  private final ObjectMapper objectMapper;

  private final UpsertAliCloudImageTagsDescription description;

  private final ClientFactory clientFactory;

  public UpsertAliCloudImageTagsAtomicOperation(
      UpsertAliCloudImageTagsDescription description,
      ObjectMapper objectMapper,
      ClientFactory clientFactory) {
    this.description = description;
    this.objectMapper = objectMapper;
    this.clientFactory = clientFactory;
  }

  @Override
  public Void operate(List priorOutputs) {
    IAcsClient client =
        clientFactory.createClient(
            description.getRegion(),
            description.getCredentials().getAccessKeyId(),
            description.getCredentials().getAccessSecretKey());
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
    describeImagesRequest.setSysRegionId(description.getRegion());
    describeImagesRequest.setImageName(description.getImageName());
    try {
      DescribeImagesResponse acsResponse = client.getAcsResponse(describeImagesRequest);
      if (acsResponse.getImages() != null && acsResponse.getImages().size() > 0) {
        TagResourcesRequest tagResourcesRequest = new TagResourcesRequest();
        List<String> imageIds = new ArrayList<>(1);
        imageIds.add(acsResponse.getImages().get(0).getImageId());
        tagResourcesRequest.setSysRegionId(description.getRegion());
        tagResourcesRequest.setResourceIds(imageIds);
        tagResourcesRequest.setResourceType("image");
        List<TagResourcesRequest.Tag> tags = new ArrayList<>(description.getTags().size());
        description
            .getTags()
            .forEach(
                (k, v) -> {
                  TagResourcesRequest.Tag tag = new TagResourcesRequest.Tag();
                  tag.setKey(k);
                  tag.setValue(v);
                  tags.add(tag);
                });
        tagResourcesRequest.setTags(tags);
        client.getAcsResponse(tagResourcesRequest);
      } else {
        throw new AliCloudException("The image not fond");
      }
    } catch (ServerException e) {
      e.printStackTrace();
      throw new AliCloudException(e.getMessage());
    } catch (ClientException e) {
      e.printStackTrace();
      throw new AliCloudException(e.getMessage());
    }
    return null;
  }
}
