/*
 * Copyright 2019 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.alicloud.deploy.ops;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsRequest;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse.ScalingGroup;
import com.aliyuncs.ess.model.v20140828.ModifyScalingGroupRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.ResizeAliCloudServerGroupDescription;
import com.netflix.spinnaker.clouddriver.alicloud.exception.AliCloudException;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import groovy.util.logging.Slf4j;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class ResizeAliCloudServerGroupAtomicOperation implements AtomicOperation<Void> {

  private final Logger log =
      LoggerFactory.getLogger(ResizeAliCloudServerGroupAtomicOperation.class);

  private final ResizeAliCloudServerGroupDescription description;

  private final ClientFactory clientFactory;

  public ResizeAliCloudServerGroupAtomicOperation(
      ResizeAliCloudServerGroupDescription description, ClientFactory clientFactory) {
    this.description = description;
    this.clientFactory = clientFactory;
  }

  @Override
  public Void operate(List priorOutputs) {
    IAcsClient client =
        clientFactory.createClient(
            description.getRegion(),
            description.getCredentials().getAccessKeyId(),
            description.getCredentials().getAccessSecretKey());
    DescribeScalingGroupsRequest describeScalingGroupsRequest = new DescribeScalingGroupsRequest();
    describeScalingGroupsRequest.setScalingGroupName(description.getServerGroupName());
    describeScalingGroupsRequest.setPageSize(50);
    DescribeScalingGroupsResponse describeScalingGroupsResponse;
    try {
      describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
      if (describeScalingGroupsResponse.getTotalCount() > 3) {
        throw new AliCloudException(
            "Alicloud search interface, Resize "
                + describeScalingGroupsResponse.getTotalCount()
                + " Scaling Groups");
      }
      for (ScalingGroup scalingGroup : describeScalingGroupsResponse.getScalingGroups()) {
        if (!AliConditionMatchUtils.match(
            describeScalingGroupsRequest.getScalingGroupName(), scalingGroup)) {
          continue;
        }
        ModifyScalingGroupRequest modifyScalingGroupRequest = new ModifyScalingGroupRequest();
        modifyScalingGroupRequest.setScalingGroupId(scalingGroup.getScalingGroupId());
        LinkedHashMap<String, Integer> capacity = description.getCapacity();
        modifyScalingGroupRequest.setMaxSize(capacity.get("max"));
        modifyScalingGroupRequest.setMinSize(capacity.get("min"));
        modifyScalingGroupRequest.setDesiredCapacity(
            capacity.get("desired") != null ? capacity.get("desired") : capacity.get("min"));
        client.getAcsResponse(modifyScalingGroupRequest);
      }
    } catch (ServerException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    } catch (ClientException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }

    return null;
  }
}
