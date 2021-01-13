package com.netflix.spinnaker.clouddriver.alicloud.deploy.converters;

import com.google.gson.Gson;
import com.netflix.spinnaker.clouddriver.alicloud.AliCloudOperation;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.BasicAliCloudDeployDescription;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.ops.CopyAliCloudServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.model.ClusterProvider;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AliCloudOperation(AtomicOperations.CLONE_SERVER_GROUP)
@Component("copyAliCloudServerGroupDescription")
public class CopyAliCloudServerGroupAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  private final ClientFactory clientFactory;

  private final List<ClusterProvider> clusterProviders;

  static final Gson gson = new Gson();

  @Autowired
  public CopyAliCloudServerGroupAtomicOperationConverter(
      ClientFactory clientFactory, List<ClusterProvider> clusterProviders) {
    this.clientFactory = clientFactory;
    this.clusterProviders = clusterProviders;
  }

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new CopyAliCloudServerGroupAtomicOperation(
        getObjectMapper(), convertDescription(input), clientFactory, clusterProviders);
  }

  @Override
  public BasicAliCloudDeployDescription convertDescription(Map input) {
    ArrayList<Map> scalingConfigurations = (ArrayList) input.get("scalingConfigurations");
    for (Map map : scalingConfigurations) {
      if (!(map.get("tags") instanceof String)) {
        map.put("tags", gson.toJson(map.get("tags")));
      }
    }
    input.put("scalingConfigurations", scalingConfigurations);
    BasicAliCloudDeployDescription description =
        getObjectMapper().convertValue(input, BasicAliCloudDeployDescription.class);
    description.setCredentials(getCredentialsObject((String) input.get("credentials")));
    return description;
  }
}
