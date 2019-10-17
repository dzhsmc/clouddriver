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
package com.netflix.spinnaker.clouddriver.alicloud.provider.agent;

import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.AUTHORITATIVE;
import static com.netflix.spinnaker.cats.agent.AgentDataType.Authority.INFORMATIVE;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.APPLICATIONS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.CLUSTERS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.INSTANCES;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.LAUNCH_CONFIGS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.LOAD_BALANCERS;
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.SERVER_GROUPS;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse.Instance;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse.SecurityGroup;
import com.aliyuncs.ess.model.v20140828.DescribeScalingConfigurationsRequest;
import com.aliyuncs.ess.model.v20140828.DescribeScalingConfigurationsResponse;
import com.aliyuncs.ess.model.v20140828.DescribeScalingConfigurationsResponse.ScalingConfiguration;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsRequest;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse;
import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse.ScalingGroup;
import com.aliyuncs.ess.model.v20140828.DescribeScalingInstancesRequest;
import com.aliyuncs.ess.model.v20140828.DescribeScalingInstancesResponse;
import com.aliyuncs.ess.model.v20140828.DescribeScalingInstancesResponse.ScalingInstance;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.slb.model.v20140515.DescribeLoadBalancerAttributeRequest;
import com.aliyuncs.slb.model.v20140515.DescribeLoadBalancerAttributeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.frigga.Names;
import com.netflix.spinnaker.cats.agent.AccountAware;
import com.netflix.spinnaker.cats.agent.AgentDataType;
import com.netflix.spinnaker.cats.agent.CacheResult;
import com.netflix.spinnaker.cats.agent.CachingAgent;
import com.netflix.spinnaker.cats.agent.DefaultCacheResult;
import com.netflix.spinnaker.cats.cache.CacheData;
import com.netflix.spinnaker.cats.cache.DefaultCacheData;
import com.netflix.spinnaker.cats.provider.ProviderCache;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudProvider;
import com.netflix.spinnaker.clouddriver.alicloud.cache.Keys;
import com.netflix.spinnaker.clouddriver.alicloud.provider.AliProvider;
import com.netflix.spinnaker.clouddriver.alicloud.security.AliCloudCredentials;
import com.netflix.spinnaker.clouddriver.cache.OnDemandAgent;
import com.netflix.spinnaker.clouddriver.cache.OnDemandMetricsSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AliCloudClusterCachingAgent implements CachingAgent, AccountAware, OnDemandAgent {

  private AliCloudCredentials account;
  private String region;
  ObjectMapper objectMapper;
  IAcsClient client;

  public AliCloudClusterCachingAgent(
      AliCloudCredentials account, String region, ObjectMapper objectMapper, IAcsClient client) {
    this.account = account;
    this.region = region;
    this.objectMapper = objectMapper;
    this.client = client;
  }

  static final Collection<AgentDataType> types =
      Collections.unmodifiableCollection(
          new ArrayList<AgentDataType>() {
            {
              add(AUTHORITATIVE.forType(CLUSTERS.ns));
              add(AUTHORITATIVE.forType(SERVER_GROUPS.ns));
              add(AUTHORITATIVE.forType(APPLICATIONS.ns));
              add(INFORMATIVE.forType(LOAD_BALANCERS.ns));
              add(INFORMATIVE.forType(LAUNCH_CONFIGS.ns));
              add(INFORMATIVE.forType(INSTANCES.ns));
            }
          });

  @Override
  public CacheResult loadData(ProviderCache providerCache) {

    DescribeScalingGroupsRequest describeScalingGroupsRequest = new DescribeScalingGroupsRequest();
    describeScalingGroupsRequest.setPageSize(50);
    DescribeScalingGroupsResponse describeScalingGroupsResponse;
    CacheResult result = new DefaultCacheResult(new HashMap<>(16));
    try {
      describeScalingGroupsResponse = client.getAcsResponse(describeScalingGroupsRequest);
      if (describeScalingGroupsResponse.getScalingGroups().size() > 0) {
        List<ScalingGroup> scalingGroups = describeScalingGroupsResponse.getScalingGroups();
        result = buildCacheResult(scalingGroups, client);
      }

    } catch (ServerException e) {
      e.printStackTrace();
    } catch (ClientException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  private CacheResult buildCacheResult(List<ScalingGroup> scalingGroups, IAcsClient client)
      throws Exception {
    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);

    Map<String, CacheData> applicationCaches = new HashMap<>(16);
    Map<String, CacheData> clusterCaches = new HashMap<>(16);
    Map<String, CacheData> serverGroupCaches = new HashMap<>(16);
    Map<String, CacheData> loadBalancerCaches = new HashMap<>(16);
    Map<String, CacheData> launchConfigCaches = new HashMap<>(16);
    Map<String, CacheData> instanceCaches = new HashMap<>(16);

    for (ScalingGroup sg : scalingGroups) {

      String activeScalingConfigurationId = sg.getActiveScalingConfigurationId();
      String scalingGroupId = sg.getScalingGroupId();
      DescribeScalingConfigurationsRequest scalingConfigurationsRequest =
          new DescribeScalingConfigurationsRequest();
      scalingConfigurationsRequest.setScalingGroupId(scalingGroupId);
      scalingConfigurationsRequest.setScalingConfigurationId1(activeScalingConfigurationId);
      DescribeScalingConfigurationsResponse scalingConfigurationsResponse =
          client.getAcsResponse(scalingConfigurationsRequest);
      List<ScalingConfiguration> scalingConfigurations =
          scalingConfigurationsResponse.getScalingConfigurations();
      String securityGroupName = "";
      // get securityGroupName
      if (scalingConfigurations.size() > 0) {
        ScalingConfiguration scalingConfiguration = scalingConfigurations.get(0);
        String securityGroupId = scalingConfiguration.getSecurityGroupId();
        DescribeSecurityGroupsRequest securityGroupsRequest = new DescribeSecurityGroupsRequest();
        securityGroupsRequest.setSecurityGroupId(securityGroupId);
        DescribeSecurityGroupsResponse securityGroupsResponse =
            client.getAcsResponse(securityGroupsRequest);
        if (securityGroupsResponse.getSecurityGroups().size() > 0) {
          SecurityGroup securityGroup = securityGroupsResponse.getSecurityGroups().get(0);
          securityGroupName = securityGroup.getSecurityGroupName();
        }
      }

      List<String> loadBalancerIds = sg.getLoadBalancerIds();
      List<DescribeLoadBalancerAttributeResponse> loadBalancerAttributes = new ArrayList<>();
      for (String loadBalancerId : loadBalancerIds) {
        try {
          DescribeLoadBalancerAttributeRequest describeLoadBalancerAttributeRequest =
              new DescribeLoadBalancerAttributeRequest();
          describeLoadBalancerAttributeRequest.setLoadBalancerId(loadBalancerId);
          DescribeLoadBalancerAttributeResponse describeLoadBalancerAttributeResponse =
              client.getAcsResponse(describeLoadBalancerAttributeRequest);
          loadBalancerAttributes.add(describeLoadBalancerAttributeResponse);
        } catch (ClientException e) {
          String message = e.getMessage();
          if (message.indexOf("InvalidLoadBalancerId.NotFound") == -1) {
            throw new IllegalStateException(e.getMessage());
          } else {
            logger.info(loadBalancerId + " -> NotFound");
          }
        }
      }

      DescribeScalingInstancesRequest scalingInstancesRequest =
          new DescribeScalingInstancesRequest();
      scalingInstancesRequest.setScalingGroupId(scalingGroupId);
      scalingInstancesRequest.setScalingConfigurationId(activeScalingConfigurationId);
      scalingInstancesRequest.setPageSize(50);
      DescribeScalingInstancesResponse scalingInstancesResponse =
          client.getAcsResponse(scalingInstancesRequest);
      List<ScalingInstance> scalingInstances = scalingInstancesResponse.getScalingInstances();
      for (ScalingInstance scalingInstance : scalingInstances) {
        if (scalingInstance.getInstanceId() != null) {
          String instanceIds = "[\"" + scalingInstance.getInstanceId() + "\"]";
          DescribeInstancesRequest request = new DescribeInstancesRequest();
          request.setInstanceIds(instanceIds);
          DescribeInstancesResponse acsResponse = client.getAcsResponse(request);
          Instance instance = acsResponse.getInstances().get(0);
          String zoneId = instance.getZoneId();
          scalingInstance.setCreationType(zoneId);
        }
      }

      SgData sgData =
          new SgData(
              sg,
              account.getName(),
              region,
              new HashMap<String, String>(16),
              scalingConfigurationsResponse,
              loadBalancerAttributes,
              scalingInstances,
              securityGroupName);

      cacheApplication(sgData, applicationCaches);
      cacheCluster(sgData, clusterCaches);
      cacheServerGroup(sgData, serverGroupCaches);
      cacheLaunchConfig(sgData, launchConfigCaches);
      cacheInstance(sgData, instanceCaches);
      cacheLoadBalancer(sgData, loadBalancerCaches);
    }

    resultMap.put(APPLICATIONS.ns, applicationCaches.values());
    resultMap.put(CLUSTERS.ns, clusterCaches.values());
    resultMap.put(SERVER_GROUPS.ns, serverGroupCaches.values());
    resultMap.put(INSTANCES.ns, instanceCaches.values());
    resultMap.put(LOAD_BALANCERS.ns, loadBalancerCaches.values());
    resultMap.put(LAUNCH_CONFIGS.ns, launchConfigCaches.values());

    return new DefaultCacheResult(resultMap);
  }

  private void cacheLoadBalancer(SgData data, Map<String, CacheData> loadBalancerCaches) {
    for (String loadBalancerName : data.loadBalancerNames) {
      CacheData oldCacheData = loadBalancerCaches.get(loadBalancerName);
      if (oldCacheData == null) {
        Map<String, Object> attributes = new HashMap<>(16);
        Map<String, Collection<String>> relationships = new HashMap<>(16);
        Set<String> serverGrouprKeys = new HashSet<>();
        serverGrouprKeys.add(data.serverGroup);
        relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);
        CacheData cacheData = new DefaultCacheData(loadBalancerName, attributes, relationships);
        loadBalancerCaches.put(loadBalancerName, cacheData);
      } else {
        CacheData cacheData = loadBalancerCaches.get(loadBalancerName);
        Map<String, Object> attributes = cacheData.getAttributes();
        Map<String, Collection<String>> relationships = cacheData.getRelationships();
        Set<String> serverGroupKeys = (Set<String>) relationships.get(SERVER_GROUPS.ns);
        serverGroupKeys.add(data.serverGroup);
        relationships.put(SERVER_GROUPS.ns, serverGroupKeys);
      }
    }
  }

  private void cacheInstance(SgData data, Map<String, CacheData> instanceCaches) {
    for (String instanceId : data.instanceIds) {
      CacheData oldCacheData = instanceCaches.get(instanceId);
      if (oldCacheData == null) {
        Map<String, Object> attributes = new HashMap<>(16);
        Map<String, Collection<String>> relationships = new HashMap<>(16);
        Set<String> serverGrouprKeys = new HashSet<>();
        serverGrouprKeys.add(data.serverGroup);
        relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);
        CacheData cacheData = new DefaultCacheData(instanceId, attributes, relationships);
        instanceCaches.put(instanceId, cacheData);
      } else {
        CacheData cacheData = instanceCaches.get(instanceId);
        Map<String, Object> attributes = cacheData.getAttributes();
        Map<String, Collection<String>> relationships = cacheData.getRelationships();
        Set<String> serverGroupKeys = (Set<String>) relationships.get(SERVER_GROUPS.ns);
        serverGroupKeys.add(data.serverGroup);
        relationships.put(SERVER_GROUPS.ns, serverGroupKeys);
      }
    }
  }

  private void cacheLaunchConfig(SgData data, Map<String, CacheData> launchConfigCaches) {
    String launchConfig = data.launchConfig;
    CacheData oldCacheData = launchConfigCaches.get(launchConfig);
    if (oldCacheData == null) {
      Map<String, Object> attributes = new HashMap<>(16);
      Map<String, Collection<String>> relationships = new HashMap<>(16);
      Set<String> serverGrouprKeys = new HashSet<>();
      serverGrouprKeys.add(data.serverGroup);
      relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);
      CacheData cacheData = new DefaultCacheData(launchConfig, attributes, relationships);
      launchConfigCaches.put(launchConfig, cacheData);
    } else {
      CacheData cacheData = launchConfigCaches.get(launchConfig);
      Map<String, Object> attributes = cacheData.getAttributes();
      Map<String, Collection<String>> relationships = cacheData.getRelationships();
      Set<String> serverGroupKeys = (Set<String>) relationships.get(SERVER_GROUPS.ns);
      serverGroupKeys.add(data.serverGroup);
      relationships.put(SERVER_GROUPS.ns, serverGroupKeys);
    }
  }

  private void cacheApplication(SgData data, Map<String, CacheData> applicationCaches) {
    String appName = data.appName;

    CacheData oldCacheData = applicationCaches.get(appName);
    if (oldCacheData == null) {
      Map<String, Object> attributes = new HashMap<>(16);
      attributes.put("name", data.name.getApp());

      Map<String, Collection<String>> relationships = new HashMap<>(16);

      Set<String> clusterKeys = new HashSet<>();
      clusterKeys.add(data.cluster);
      relationships.put(CLUSTERS.ns, clusterKeys);

      Set<String> serverGrouprKeys = new HashSet<>();
      serverGrouprKeys.add(data.serverGroup);
      relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);

      Set<String> loadBalancerKeys = new HashSet<>();
      loadBalancerKeys.addAll(data.loadBalancerNames);
      relationships.put(LOAD_BALANCERS.ns, loadBalancerKeys);

      CacheData cacheData = new DefaultCacheData(appName, attributes, relationships);

      applicationCaches.put(appName, cacheData);
    } else {
      CacheData cacheData = applicationCaches.get(appName);
      Map<String, Object> attributes = cacheData.getAttributes();
      attributes.put("name", data.name.getApp());

      Map<String, Collection<String>> relationships = cacheData.getRelationships();

      Set<String> clusterKeys = (Set<String>) relationships.get(CLUSTERS.ns);
      clusterKeys.add(data.cluster);

      Set<String> serverGrouprKeys = (Set<String>) relationships.get(SERVER_GROUPS.ns);
      serverGrouprKeys.add(data.serverGroup);

      Set<String> loadBalancerKeys = (Set<String>) relationships.get(LOAD_BALANCERS.ns);
      loadBalancerKeys.addAll(data.loadBalancerNames);
    }
  }

  private void cacheCluster(SgData data, Map<String, CacheData> clusterCaches) {
    String cluster = data.cluster;

    CacheData oldCacheData = clusterCaches.get(cluster);
    if (oldCacheData == null) {
      Map<String, Object> attributes = new HashMap<>(16);
      attributes.put("name", data.name.getCluster());
      attributes.put("application", data.name.getApp());

      Map<String, Collection<String>> relationships = new HashMap<>(16);

      Set<String> applicationKeys = new HashSet<>();
      applicationKeys.add(data.appName);
      relationships.put(APPLICATIONS.ns, applicationKeys);

      Set<String> serverGrouprKeys = new HashSet<>();
      serverGrouprKeys.add(data.serverGroup);
      relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);

      Set<String> loadBalancerKeys = new HashSet<>();
      loadBalancerKeys.addAll(data.loadBalancerNames);
      relationships.put(LOAD_BALANCERS.ns, loadBalancerKeys);

      CacheData cacheData = new DefaultCacheData(cluster, attributes, relationships);

      clusterCaches.put(cluster, cacheData);
    } else {
      CacheData cacheData = clusterCaches.get(cluster);
      Map<String, Object> attributes = cacheData.getAttributes();
      attributes.put("name", data.name.getCluster());
      attributes.put("application", data.name.getApp());

      Map<String, Collection<String>> relationships = cacheData.getRelationships();

      Set<String> applicationKeys = (Set<String>) relationships.get(APPLICATIONS.ns);
      applicationKeys.add(data.appName);
      relationships.put(APPLICATIONS.ns, applicationKeys);

      Set<String> serverGrouprKeys = (Set<String>) relationships.get(SERVER_GROUPS.ns);
      serverGrouprKeys.add(data.serverGroup);
      relationships.put(SERVER_GROUPS.ns, serverGrouprKeys);

      Set<String> loadBalancerKeys = (Set<String>) relationships.get(LOAD_BALANCERS.ns);
      loadBalancerKeys.addAll(data.loadBalancerNames);
      relationships.put(LOAD_BALANCERS.ns, loadBalancerKeys);
    }
  }

  private void cacheServerGroup(SgData data, Map<String, CacheData> serverGroupCaches) {
    String serverGroup = data.serverGroup;

    Map<String, Object> attributes = new HashMap<>(16);
    attributes.put("application", data.name.getApp());
    attributes.put("scalingGroup", data.sg);
    attributes.put("region", region);
    attributes.put("name", data.sg.getScalingGroupName());
    if (data.scalingConfigurationsResponse.getScalingConfigurations().size() > 0) {
      attributes.put(
          "launchConfigName",
          data.scalingConfigurationsResponse
              .getScalingConfigurations()
              .get(0)
              .getScalingConfigurationName());
      ScalingConfiguration scalingConfiguration =
          data.scalingConfigurationsResponse.getScalingConfigurations().get(0);
      Map<String, Object> map = objectMapper.convertValue(scalingConfiguration, Map.class);
      map.put("securityGroupName", data.securityGroupName);
      attributes.put("scalingConfiguration", map);
    } else {
      attributes.put("scalingConfiguration", new ScalingConfiguration());
    }
    attributes.put("instances", data.scalingInstances);
    attributes.put("loadBalancers", data.loadBalancerAttributes);
    attributes.put("provider", AliCloudProvider.ID);
    attributes.put("account", account.getName());
    attributes.put("regionId", region);

    Map<String, Collection<String>> relationships = new HashMap<>(16);

    Set<String> applicationKeys = new HashSet<>();
    applicationKeys.add(data.appName);
    relationships.put(APPLICATIONS.ns, applicationKeys);

    Set<String> clusterKeys = new HashSet<>();
    clusterKeys.add(data.cluster);
    relationships.put(CLUSTERS.ns, clusterKeys);

    Set<String> loadBalancerKeys = new HashSet<>();
    loadBalancerKeys.addAll(data.loadBalancerNames);
    relationships.put(LOAD_BALANCERS.ns, loadBalancerKeys);

    Set<String> launchConfigsKeys = new HashSet<>();
    launchConfigsKeys.add(data.launchConfig);
    relationships.put(LAUNCH_CONFIGS.ns, launchConfigsKeys);

    Set<String> instancesKeys = new HashSet<>();
    instancesKeys.addAll(data.instanceIds);
    relationships.put(INSTANCES.ns, instancesKeys);

    CacheData cacheData = new DefaultCacheData(serverGroup, attributes, relationships);

    serverGroupCaches.put(serverGroup, cacheData);
  }

  private static class SgData {
    final ScalingGroup sg;
    final DescribeScalingConfigurationsResponse scalingConfigurationsResponse;
    final List<ScalingInstance> scalingInstances;
    final List<DescribeLoadBalancerAttributeResponse> loadBalancerAttributes;
    final Names name;
    final String appName;
    final String cluster;
    final String serverGroup;
    final String launchConfig;
    final Set<String> loadBalancerNames = new HashSet<>();
    final Set<String> instanceIds = new HashSet<>();
    final String securityGroupName;

    public SgData(
        ScalingGroup sg,
        String account,
        String region,
        Map<String, String> subnetMap,
        DescribeScalingConfigurationsResponse scalingConfigurationsResponse,
        List<DescribeLoadBalancerAttributeResponse> loadBalancerAttributes,
        List<ScalingInstance> scalingInstances,
        String securityGroupName) {

      this.sg = sg;
      this.scalingConfigurationsResponse = scalingConfigurationsResponse;
      this.scalingInstances = scalingInstances;
      this.loadBalancerAttributes = loadBalancerAttributes;
      name = Names.parseName(sg.getScalingGroupName());
      appName = Keys.getApplicationKey(name.getApp());
      cluster = Keys.getClusterKey(name.getCluster(), name.getApp(), account);
      serverGroup = Keys.getServerGroupKey(sg.getScalingGroupName(), account, region);
      launchConfig = Keys.getLaunchConfigKey(sg.getScalingGroupName(), account, region);
      for (DescribeLoadBalancerAttributeResponse loadBalancerAttribute : loadBalancerAttributes) {
        loadBalancerNames.add(
            Keys.getLoadBalancerKey(
                loadBalancerAttribute.getLoadBalancerName(), account, region, null));
      }
      for (ScalingInstance scalingInstance : scalingInstances) {
        instanceIds.add(Keys.getInstanceKey(scalingInstance.getInstanceId(), account, region));
      }
      this.securityGroupName = securityGroupName;
    }
  }

  @Override
  public OnDemandResult handle(ProviderCache providerCache, Map<String, ?> data) {
    // TODO this is a same
    return null;
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return AliProvider.PROVIDER_NAME;
  }

  @Override
  public String getAccountName() {
    return account.getName();
  }

  @Override
  public String getOnDemandAgentType() {
    return this.getAgentType() + "-OnDemand";
  }

  @Override
  public OnDemandMetricsSupport getMetricsSupport() {
    return null;
  }

  @Override
  public boolean handles(OnDemandType type, String cloudProvider) {
    return false;
  }

  @Override
  public Collection<Map> pendingOnDemandRequests(ProviderCache providerCache) {
    return null;
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }
}
