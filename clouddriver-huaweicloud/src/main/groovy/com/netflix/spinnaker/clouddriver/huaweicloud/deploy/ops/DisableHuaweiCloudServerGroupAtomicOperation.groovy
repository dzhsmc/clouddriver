package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/huaweicloud/ops \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '[
    {
        "disableServerGroup": {
        	"accountName": "test",
            "serverGroupName": "myapp-dev-v007",
            "region": "ap-guangzhou",
            "credentials": "test"
        }
    }
]'
*/

class DisableHuaweiCloudServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "DISABLE_SERVER_GROUP"
  boolean disable = true

  DisableHuaweiCloudServerGroupAtomicOperation(EnableDisableHuaweiCloudServerGroupDescription description) {
    super(description)
  }
}
