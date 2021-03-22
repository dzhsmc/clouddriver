package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException
import com.netflix.spinnaker.clouddriver.huaweicloud.model.HuaweiCloudSecurityGroupRule

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.vpc.v2.VpcClient
import com.huaweicloud.sdk.vpc.v2.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class HuaweiVirtualPrivateCloudClient {
  private final DEFAULT_LIMIT = 100
  VpcClient client

  HuaweiVirtualPrivateCloudClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://vpc." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = VpcClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  String createSecurityGroup(String groupName) {
    try{
      CreateSecurityGroupRequest req = new CreateSecurityGroupRequest()
      CreateSecurityGroupRequestBody body = new CreateSecurityGroupRequestBody()
      CreateSecurityGroupOption securityGroupbody = new CreateSecurityGroupOption()
      securityGroupbody.setName(groupName)
      body.setSecurityGroup(securityGroupbody);
      req.setBody(body);
      CreateSecurityGroupResponse resp = client.CreateSecurityGroup(req)
      return resp.getSecurityGroup().getId()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  String createSecurityGroupRule(String groupId, HuaweiCloudSecurityGroupRule inRule) {
    try{
      CreateSecurityGroupRuleRequest req = new CreateSecurityGroupRuleRequest()
      CreateSecurityGroupRuleRequestBody body = new CreateSecurityGroupRuleRequestBody()
      CreateSecurityGroupRuleOption securityGroupRulebody = new CreateSecurityGroupRuleOption()
      securityGroupRulebody.withSecurityGroupId(groupId)
          .withDirection("ingress");

      if (inRule.getIpProtocol() != null) {
        securityGroupRulebody.setProtocol(inRule.getIpProtocol());
      }
      if (inRule.getStartPortRange() != null) {
        securityGroupRulebody.setPortRangeMin(inRule.getStartPortRange())
      }
      if (inRule.getEndPortRange() != null) {
        securityGroupRulebody.setPortRangeMax(inRule.getEndPortRange())
      }
      if (inRule.getSourceCidrIp() != null) {
        securityGroupRulebody.setRemoteIpPrefix(inRule.getSourceCidrIp())
      }
      if (inRule.getRemoteGroupId() != null) {
        securityGroupRulebody.setRemoteGroupId(inRule.getRemoteGroupId())
      }

      body.withSecurityGroupRule(securityGroupRulebody)
      req.setBody(body)
      client.createSecurityGroupRule(req)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
    return ""
  }

  String deleteSecurityGroupInRule(String ruleId) {
    try{
      DeleteSecurityGroupRuleRequest req = new DeleteSecurityGroupRuleRequest()
      req.setSecurityGroupRuleId(ruleId)
      client.deleteSecurityGroupRule(request);
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
    return ""
  }

  List<SecurityGroup> getSecurityGroupsAll() {
    def marker = ""
    List<SecurityGroup> securityGroupAll = []
    try{
      while(true) {
        def req = new ListSecurityGroupsRequest()
        req.setLimit(DEFAULT_LIMIT)
        if (marker) {
          req.setMarker(marker)
        }
        def resp = client.listSecurityGroups(req)
        if(resp == null || resp.getSecurityGroups() == null || resp.getSecurityGroups().size() == 0) {
          break
        }
        def sgs = resp.getSecurityGroups()
        securityGroupAll.addAll(sgs)
        marker = sgs[sgs.size() - 1].getId()
      }
      return securityGroupAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<SecurityGroup> getSecurityGroupById(String securityGroupId) {
    try{
      ShowSecurityGroupRequest req = new ShowSecurityGroupRequest()
      req.setSecurityGroupId(securityGroupId)
      ShowSecurityGroupResponse resp = client.showSecurityGroup(req)
      return resp.getSecurityGroup()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  void deleteSecurityGroup(String securityGroupId) {
    try{
      DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest()
      req.setSecurityGroupId(securityGroupId)
      client.DeleteSecurityGroup(req)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Vpc> getNetworksAll() {
    def marker = ""
    List<Vpc> networkAll =[]
    try{
      while(true) {
        def req = new ListVpcsRequest().withLimit(DEFAULT_LIMIT)
        if (marker) {
          req.setMarker(marker)
        }
        def  resp = client.listVpcs(req)
        if(resp == null || resp.getVpcs() == null || resp.getVpcs().size() == 0) {
          break
        }
        def vpcs = resp.getVpcs()
        networkAll.addAll(vpcs)
        marker = vpcs[vpcs.size() - 1].getId()
      }
      return networkAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Subnet> getSubnetsAll() {
    def marker = ""
    List<Subnet> subnetAll = []
    try{
      while(true) {
        def req = new ListSubnetsRequest()
        req.setLimit(DEFAULT_LIMIT)
        if (marker) {
          req.setMarker(marker)
        }
        def resp = client.listSubnets(req)
        if(resp == null || resp.getSubnets() == null || resp.getSubnets().size() == 0) {
          break
        }
        def subnets = resp.getSubnets()
        subnetAll.addAll(subnets)
        marker = subnets[subnets.size() - 1].getId()
      }
      return subnetAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  Subnet getSubnet(String subnetId) {
    def req = new ShowSubnetRequest().withSubnetId(subnetId)
    try{
      def resp = client.showSubnet(req)
      return resp.getSubnet()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  Port getPort(String portId) {
    def req = new ShowPortRequest().withPortId(portId)
    try{
      def resp = client.showPort(req)
      return resp.getPort()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Port> getAllPorts() {
    def marker = ""
    List<Port> portAll = []
    try{
      while(true) {
        def req = new ListPortsRequest().withLimit(DEFAULT_LIMIT)
        if (marker) {
          req.setMarker(marker)
        }
        def resp = client.listPorts(req)
        if(resp == null || resp.getPorts() == null || resp.getPorts().size() == 0) {
          break
        }
        def ports = resp.getPorts()
        portAll.addAll(ports)
        marker = ports[ports.size() - 1].getId()
      }
      return portAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

}
