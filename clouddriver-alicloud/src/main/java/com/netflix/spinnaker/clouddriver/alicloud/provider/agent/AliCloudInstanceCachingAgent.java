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
import static com.netflix.spinnaker.clouddriver.core.provider.agent.Namespace.INSTANCES;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
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
import java.util.*;
import org.springframework.util.CollectionUtils;

public class AliCloudInstanceCachingAgent implements CachingAgent {

  AliCloudCredentials account;
  String region;
  ObjectMapper objectMapper;
  IAcsClient client;

  public AliCloudInstanceCachingAgent(
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
              add(AUTHORITATIVE.forType(INSTANCES.ns));
            }
          });

  @Override
  public CacheResult loadData(ProviderCache providerCache) {
    Map<String, Collection<CacheData>> resultMap = new HashMap<>(16);
    List<CacheData> instanceDatas = new ArrayList<>();

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    describeInstancesRequest.setSysRegionId(region);
    int pageNumber = 1;
    int pageSize = 10;

    DescribeInstancesResponse describeInstancesResponse;
    try {
      while (true) {
        describeInstancesRequest.setPageNumber(pageNumber);
        describeInstancesRequest.setPageSize(pageSize);
        describeInstancesResponse = client.getAcsResponse(describeInstancesRequest);
        if (!CollectionUtils.isEmpty(describeInstancesResponse.getInstances())) {
          pageNumber = pageNumber + 1;
          for (DescribeInstancesResponse.Instance instance :
              describeInstancesResponse.getInstances()) {
            String zoneId = instance.getZoneId();
            String regionId = instance.getBizRegionId();
            String instanceId = instance.getInstanceId();

            Map<String, Object> attributes = objectMapper.convertValue(instance, Map.class);
            attributes.put("provider", AliCloudProvider.ID);
            attributes.put("account", account.getName());
            attributes.put("regionId", regionId);
            attributes.put("zoneId", zoneId);

            CacheData data =
                new DefaultCacheData(
                    Keys.getInstanceKey(instanceId, account.getName(), regionId),
                    attributes,
                    new HashMap<>(16));
            instanceDatas.add(data);
          }
        } else {
          break;
        }
      }
    } catch (ServerException e) {
      e.printStackTrace();
    } catch (ClientException e) {
      e.printStackTrace();
    }

    resultMap.put(INSTANCES.ns, instanceDatas);

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
