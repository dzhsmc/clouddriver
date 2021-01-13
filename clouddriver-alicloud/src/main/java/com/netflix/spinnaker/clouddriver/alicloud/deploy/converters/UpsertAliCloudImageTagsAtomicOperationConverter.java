package com.netflix.spinnaker.clouddriver.alicloud.deploy.converters;

import com.netflix.spinnaker.clouddriver.alicloud.AliCloudOperation;
import com.netflix.spinnaker.clouddriver.alicloud.common.ClientFactory;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.description.UpsertAliCloudImageTagsDescription;
import com.netflix.spinnaker.clouddriver.alicloud.deploy.ops.UpsertAliCloudImageTagsAtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation;
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations;
import com.netflix.spinnaker.clouddriver.security.AbstractAtomicOperationsCredentialsSupport;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AliCloudOperation(AtomicOperations.UPSERT_IMAGE_TAGS)
@Component("upsertAliCloudImageTagsDescription")
public class UpsertAliCloudImageTagsAtomicOperationConverter
    extends AbstractAtomicOperationsCredentialsSupport {

  private final ClientFactory clientFactory;

  @Autowired
  public UpsertAliCloudImageTagsAtomicOperationConverter(ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public AtomicOperation convertOperation(Map input) {
    return new UpsertAliCloudImageTagsAtomicOperation(
        convertDescription(input), getObjectMapper(), clientFactory);
  }

  @Override
  public UpsertAliCloudImageTagsDescription convertDescription(Map input) {
    UpsertAliCloudImageTagsDescription converted =
        getObjectMapper().convertValue(input, UpsertAliCloudImageTagsDescription.class);
    converted.setCredentials(getCredentialsObject((String) input.get("credentials")));
    return converted;
  }
}
