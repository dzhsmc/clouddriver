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
import com.aliyuncs.ess.model.v20140828.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudProvider;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.AliCloudServerGroupNameResolver;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.BasicAliCloudDeployDescription;
import com.netflix.spinnaker.clouddriver.alicloud.exception.AliCloudException;
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult;
import com.netflix.spinnaker.clouddriver.model.ClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import groovy.util.logging.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class CreateAliCloudServerGroupAtomicOperation implements AtomicOperation<DeploymentResult> {

  private final Logger log =
      LoggerFactory.getLogger(CreateAliCloudServerGroupAtomicOperation.class);

  private final List<ClusterProvider> clusterProviders;

  private final ObjectMapper objectMapper;

  private final BasicAliCloudDeployDescription description;

  private final ClientFactory clientFactory;

  public CreateAliCloudServerGroupAtomicOperation(
      BasicAliCloudDeployDescription description,
      ObjectMapper objectMapper,
      ClientFactory clientFactory,
      List<ClusterProvider> clusterProviders) {
    this.description = description;
    this.objectMapper = objectMapper;
    this.clientFactory = clientFactory;
    this.clusterProviders = clusterProviders;
  }

  @Override
  public DeploymentResult operate(List priorOutputs) {
    DeploymentResult result = new DeploymentResult();
    // create scaling group
    IAcsClient client =
        clientFactory.createClient(
            description.getRegion(),
            description.getCredentials().getAccessKeyId(),
            description.getCredentials().getAccessSecretKey());
    AliCloudServerGroupNameResolver resolver =
        new AliCloudServerGroupNameResolver(
            description.getCredentials().getName(), description.getRegion(), clusterProviders);
    String serverGroupName =
        resolver.resolveNextServerGroupName(
            description.getApplication(),
            description.getStack(),
            description.getFreeFormDetails(),
            false);
    description.setScalingGroupName(serverGroupName);
    CreateScalingGroupRequest createScalingGroupRequest =
        objectMapper.convertValue(description, CreateScalingGroupRequest.class);
    rebuildCreateScalingGroupRequest(description, createScalingGroupRequest);
    createScalingGroupRequest.setScalingGroupName(serverGroupName);
    if (!StringUtils.isEmpty(description.getVSwitchId())) {
      createScalingGroupRequest.setVSwitchId(description.getVSwitchId());
    }
    if (description.getVSwitchIds() != null) {
      createScalingGroupRequest.setVSwitchIds(description.getVSwitchIds());
    }
    CreateScalingGroupResponse createScalingGroupResponse;
    try {
      createScalingGroupResponse = client.getAcsResponse(createScalingGroupRequest);
      description.setScalingGroupId(createScalingGroupResponse.getScalingGroupId());
    } catch (ServerException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    } catch (ClientException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }

    if (StringUtils.isEmpty(description.getScalingGroupId())) {
      return result;
    }

    String scalingConfigurationId = null;

    // create scaling configuration
    for (CreateScalingConfigurationRequest scalingConfiguration :
        description.getScalingConfigurations()) {
      CreateScalingConfigurationRequest configurationRequest =
          objectMapper.convertValue(scalingConfiguration, CreateScalingConfigurationRequest.class);
      configurationRequest.setScalingGroupId(description.getScalingGroupId());
      CreateScalingConfigurationResponse configurationResponse;
      try {
        configurationResponse = client.getAcsResponse(configurationRequest);
        scalingConfigurationId = configurationResponse.getScalingConfigurationId();
      } catch (ServerException e) {
        log.info(e.getMessage());
        throw new AliCloudException(e.getMessage());
      } catch (ClientException e) {
        log.info(e.getMessage());
        throw new AliCloudException(e.getMessage());
      }
    }

    if (StringUtils.isEmpty(scalingConfigurationId)) {
      return result;
    }

    EnableScalingGroupRequest enableScalingGroupRequest = new EnableScalingGroupRequest();
    enableScalingGroupRequest.setScalingGroupId(description.getScalingGroupId());
    enableScalingGroupRequest.setActiveScalingConfigurationId(scalingConfigurationId);
    EnableScalingGroupResponse enableScalingGroupResponse;
    try {
      enableScalingGroupResponse = client.getAcsResponse(enableScalingGroupRequest);
    } catch (ServerException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    } catch (ClientException e) {
      log.info(e.getMessage());
      throw new AliCloudException(e.getMessage());
    }

    buildResult(description, result);

    return result;
  }

  private void rebuildCreateScalingGroupRequest(
      BasicAliCloudDeployDescription description, CreateScalingGroupRequest request) {
    if (description != null
        && description.getSource() != null
        && description.getSource().getUseSourceCapacity()
        && StringUtils.isNotEmpty(description.getSource().getAsgName())) {

      String asgName = description.getSource().getAsgName();
      DescribeScalingGroupsRequest describeScalingGroupsRequest =
          new DescribeScalingGroupsRequest();
      describeScalingGroupsRequest.setScalingGroupName(asgName);
      DescribeScalingGroupsResponse describeScalingGroupsResponse;
      try {
        IAcsClient client =
            clientFactory.createClient(
                description.getRegion(),
                description.getCredentials().getAccessKeyId(),
                description.getCredentials().getAccessSecretKey());

        describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
        if (describeScalingGroupsResponse.getScalingGroups().size() == 0) {
          throw new AliCloudException("Old server group is does not exist");
        }
        DescribeScalingGroupsResponse.ScalingGroup scalingGroup =
            describeScalingGroupsResponse.getScalingGroups().get(0);
        if (scalingGroup.getMaxSize() != null) {
          request.setMaxSize(scalingGroup.getMaxSize());
        }
        if (scalingGroup.getMinSize() != null) {
          request.setMinSize(scalingGroup.getMinSize());
        }
      } catch (Exception e) {
        log.info(e.getMessage());
        throw new AliCloudException(e.getMessage());
      }
    }
  }

  private void buildResult(BasicAliCloudDeployDescription description, DeploymentResult result) {

    List<String> serverGroupNames = new ArrayList<>();
    serverGroupNames.add(description.getRegion() + ":" + description.getScalingGroupName());
    result.setServerGroupNames(serverGroupNames);

    Map<String, String> serverGroupNameByRegion = new HashMap<>();
    serverGroupNameByRegion.put(description.getRegion(), description.getScalingGroupName());
    result.setServerGroupNameByRegion(serverGroupNameByRegion);

    Set<DeploymentResult.Deployment> deployments = new HashSet<>();

    DeploymentResult.Deployment.Capacity capacity = new DeploymentResult.Deployment.Capacity();
    capacity.setMax(description.getMaxSize());
    capacity.setMin(description.getMinSize());
    capacity.setDesired(description.getMinSize());

    DeploymentResult.Deployment deployment = new DeploymentResult.Deployment();
    deployment.setCloudProvider(AliCloudProvider.ID);
    deployment.setAccount(description.getCredentials().getName());
    deployment.setCapacity(capacity);
    deployment.setLocation(description.getRegion());
    deployment.setServerGroupName(description.getScalingGroupName());

    deployments.add(deployment);
    result.setDeployments(deployments);
  }
}
