package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.netflix.spinnaker.clouddriver.model.SecurityGroup
import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.clouddriver.huaweicloud.HuaweiCloudProvider
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.Immutable


@Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class HuaweiCloudSecurityGroup implements SecurityGroup{
  final String type = HuaweiCloudProvider.ID
  final String cloudProvider = HuaweiCloudProvider.ID
  final String id             //securityGroupId
  final String name           //securityGroupName
  final String description    //securityGroupDesc
  final String application
  final String accountName
  final String region

  final Set<Rule> inboundRules = []
  final Set<Rule> outboundRules = []

  List<HuaweiCloudSecurityGroupRule> inRules
  List<HuaweiCloudSecurityGroupRule> outRules

  void setMoniker(Moniker _ignored) {}

  @Override
  SecurityGroupSummary getSummary() {
    new HuaweiCloudSecurityGroupSummary(name: name, id: id)
  }
}
