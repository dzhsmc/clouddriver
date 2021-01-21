package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.EnableDisableHuaweiCloudServerGroupDescription

/*
curl -X POST \
  http://localhost:7002/huaweicloud/ops \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '[
    {
        "enableServerGroup": {
        	"accountName": "test",
            "serverGroupName": "myapp-dev-v007",
            "region": "ap-guangzhou",
            "credentials": "test"
        }
    }
]'
*/

class EnableHuaweiCloudServerGroupAtomicOperation extends AbstractEnableDisableAtomicOperation {
  final String basePhase = "ENABLE_SERVER_GROUP"
  final boolean disable = false

  EnableHuaweiCloudServerGroupAtomicOperation (EnableDisableHuaweiCloudServerGroupDescription description) {
    super(description)
  }
}
