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

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone.AvailableResource;
import com.aliyuncs.ecs.model.v20140526.DescribeAvailableResourceResponse.AvailableZone.AvailableResource.SupportedResource;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AliCloudInstanceTypeCachingAgent implements CachingAgent {

  AliCloudCredentials account;
  String region;
  ObjectMapper objectMapper;
  IAcsClient client;

  public AliCloudInstanceTypeCachingAgent(
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
              add(AUTHORITATIVE.forType(Keys.Namespace.INSTANCE_TYPES.ns));
            }
          });

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    List<CacheData> instanceTypeDatas = new ArrayList<>();

    DescribeAvailableResourceRequest describeZonesRequest = new DescribeAvailableResourceRequest();
    describeZonesRequest.setDestinationResource("InstanceType");
    describeZonesRequest.setInstanceChargeType("PostPaid");
    describeZonesRequest.setIoOptimized("optimized");
    describeZonesRequest.setResourceType("instance");

    DescribeAvailableResourceResponse describeZonesResponse;
    try {
      describeZonesResponse = client.getAcsResponse(describeZonesRequest);
      for (AvailableZone availableZone : describeZonesResponse.getAvailableZones()) {
        List<String> names = new ArrayList();
        String status = availableZone.getStatus();
        String statusCategory = availableZone.getStatusCategory();
        String regionId = availableZone.getRegionId();
        String zoneId = availableZone.getZoneId();
        if ("Available".equals(status) && !"WithoutStock".equals(statusCategory)) {
          for (AvailableResource availableResource : availableZone.getAvailableResources()) {
            String type = availableResource.getType();
            if ("InstanceType".equals(type)) {
              for (SupportedResource supportedResource :
                  availableResource.getSupportedResources()) {
                String value = supportedResource.getValue();
                String thisStatus = supportedResource.getStatus();
                String thisStatusCategory = supportedResource.getStatusCategory();
                if ("Available".equals(thisStatus) && !"WithoutStock".equals(thisStatusCategory)) {
                  names.add(value);
                }
              }
            }
          }
        }
        Map<String, Object> attributes = new HashMap<>(20);
        attributes.put("provider", AliCloudProvider.ID);
        attributes.put("account", account.getName());
        attributes.put("regionId", regionId);
        attributes.put("zoneId", zoneId);
        attributes.put("names", names);
        CacheData data =
            new DefaultCacheData(
                Keys.getInstanceTypeKey(account.getName(), region, zoneId),
                attributes,
                new HashMap<>(16));
        instanceTypeDatas.add(data);
      }
    } catch (ServerException e) {
      e.printStackTrace();
    } catch (ClientException e) {
      e.printStackTrace();
    }

    resultMap.put(Keys.Namespace.INSTANCE_TYPES.ns, instanceTypeDatas);

    return new DefaultCacheResult(resultMap);
  }

  @Override
  public Collection<AgentDataType> getProvidedDataTypes() {
    return types;
  }

  @Override
  public String getAgentType() {
    return account.getName() + "/" + region + "/" + this.getClass().getSimpleName();
  }

  @Override
  public String getProviderName() {
    return AliProvider.PROVIDER_NAME;
  }
}
