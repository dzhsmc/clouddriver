package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.deploy.description.UpsertHuaweiCloudLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerRule
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.elb.v2.ElbClient
import com.huaweicloud.sdk.elb.v2.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class LoadBalancerClient {
  private final DEFAULT_LIMIT = 100
  ElbClient client

  LoadBalancerClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://elb." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = ElbClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  List<LoadbalancerResp> getAllLoadBalancer() {
    List<LoadbalancerResp> loadBalancerAll = []
    try {
      def req = new ListLoadbalancersRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listLoadbalancers(req)
      def loadBalancers = resp.getLoadbalancers()
      loadBalancerAll.addAll(loadBalancers)
      while (loadBalancers.size() == DEFAULT_LIMIT) {
        req.setMarker(loadBalancers[loadBalancers.size() - 1].getId())
        resp = client.listLoadbalancers(req)
        loadBalancers = resp.getLoadbalancers()
        loadBalancerAll.addAll(loadBalancers)
      }
      return loadBalancerAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<LoadbalancerResp> getLoadBalancerByName(String name) {
    try{
      def req = new ListLoadbalancersRequest().withName(name)
      def resp = client.listLoadbalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<LoadbalancerResp> getLoadBalancerById(String id) {
    try{
      def req = new ListLoadbalancersRequest().withId(id)
      def resp = client.listLoadbalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  String deleteLoadBalancerById(String loadBalancerId) {
    try{
      def req = new DeleteLoadbalancerRequest()
      req.setLoadbalancerId(loadBalancerId)
      client.deleteLoadbalancer(req)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  String createLoadBalancer(UpsertHuaweiCloudLoadBalancerDescription description) {
    try{
      def req = new CreateLoadbalancerRequest();
      def body = new CreateLoadbalancerRequestBody();
      def reqBody = new CreateLoadbalancerReq()
      reqBody.setName(description.loadBalancerName)
      if (description.subnetId?.length() > 0) {
        reqBody.setVipSubnetId(description.subnetId)
      }
      body.withLoadbalancer(reqBody)
      req.withBody(body)
      def resp = client.createLoadbalancer(req)
      return resp.getLoadbalancer().getId()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  List<ListenerResp> getAllLBListener(List<String> listenerIds) {
    List<ListenerResp> listenerAll = []
    try{
      listenerIds.each {
        def req = new ShowListenerRequest().withListenerId(it)
        def resp = client.showListener(req);
        listenerAll.add(resp.getListener())
      }
      return listenerAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

 String createLBListener(String loadBalancerId, HuaweiCloudLoadBalancerListener listener) {
  try{
    // step1: create listener
    def req = new CreateListenerRequest()
    def body = new CreateListenerRequestBody()
    def reqBody= new CreateListenerReq()
    reqBody.setLoadbalancerId(loadBalancerId)
    reqBody.setProtocolPort(listener.port)
    // set protocol
    if (listener.protocol == "TCP") {
      reqBody.setProtocol(CreateListenerReq.ProtocolEnum.TCP)
    } else if (listener.protocol == "UDP") {
      reqBody.setProtocol(CreateListenerReq.ProtocolEnum.UDP)
    } else if (listener.protocol == "HTTP") {
      reqBody.setProtocol(CreateListenerReq.ProtocolEnum.HTTP)
    } else if (listener.protocol == "HTTPS") {
      reqBody.setProtocol(CreateListenerReq.ProtocolEnum.TERMINATED_HTTPS)
    }
    // set name
    def listenerName = listener.protocol + listener.port
    if (listener.listenerName?.length() > 0 ) {
      listenerName = listener.listenerName
    }
    reqBody.setName(listenerName)
    // set certificate
    if (listener.certificate != null ) {
      if (listener.certificate.certId?.length() > 0) {
        reqBody.setDefaultTlsContainerRef(listener.certificate.certId)
      }
      if (listener.certificate.certCaId?.length() > 0) {
        reqBody.setClientCaTlsContainerRef(listener.certificate.certCaId)
      }
    }
    body.withListener(reqBody)
    req.withBody(body)
    def resp = client.createListener(req)
    def listenerId = resp.getListener().getId()

    // step2: create layer4 pool and healthcheck
    if (listener.protocol in ["TCP","UDP"]) {
      def poolId = createPool(listenerId, listener.protocol, "L4")
      if (poolId?.length() > 0) {
        createL4HealthCheck(poolId, listener)
      }
    }
    return listenerId 
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
    return ""
  }

  private String createPool(String id, String protocol, String layer) {
    try{
      def req = new CreatePoolRequest()
      def body = new CreatePoolRequestBody()
      def reqBody = new CreatePoolReq()
      // set protocol
      if (protocol == "TCP") {
        reqBody.setProtocol(CreatePoolReq.ProtocolEnum.TCP)
      } else if (protocol == "UDP") {
        reqBody.setProtocol(CreatePoolReq.ProtocolEnum.UDP)
      } else if (protocol == "HTTP") {
        reqBody.setProtocol(CreatePoolReq.ProtocolEnum.HTTP)
      } else if (protocol == "HTTPS") {
        reqBody.setProtocol(CreatePoolReq.ProtocolEnum.HTTP)
      }
      reqBody.setLbAlgorithm("ROUND_ROBIN")
      if (layer == "L4") {
        reqBody.setListenerId(id)
      } else if(layer == "L7") {
        reqBody.setLoadbalancerId(id)
      }
      body.withPool(reqBody)
      req.withBody(body)
      def resp = client.createPool(req)
      return resp.getPool().getId()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  private String createL4HealthCheck(String poolId, HuaweiCloudLoadBalancerListener listener) {
    try{
      def req = new CreateHealthmonitorRequest()
      def body = new CreateHealthmonitorRequestBody()
      def reqBody = new CreateHealthmonitorReq()
      reqBody.setPoolId(poolId)
      reqBody.setDelay(listener.healthCheck.intervalTime)
      reqBody.setMaxRetries(listener.healthCheck.maxRetries)
      reqBody.setTimeout(listener.healthCheck.timeOut)
      // set protocol type
      if (listener.protocol == "TCP") {
        reqBody.setType(CreateHealthmonitorReq.TypeEnum.TCP)
      } else if (listener.protocol == "UDP") {
        reqBody.setType(CreateHealthmonitorReq.TypeEnum.UDP_CONNECT)
      }
      body.withHealthmonitor(reqBody)
      req.withBody(body)
      def resp = client.createHealthmonitor(req)
      return resp.getHealthmonitor().getId()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  private String createL7HealthCheck(String poolId, HuaweiCloudLoadBalancerRule rule) {
    try{
      def req = new CreateHealthmonitorRequest()
      def body = new CreateHealthmonitorRequestBody()
      def reqBody = new CreateHealthmonitorReq()
      reqBody.setPoolId(poolId)
      reqBody.setDelay(rule.healthCheck.intervalTime)
      reqBody.setMaxRetries(rule.healthCheck.maxRetries)
      reqBody.setTimeout(rule.healthCheck.timeOut)
      if (rule.healthCheck.httpCheckDomain?.length() > 0) {
        reqBody.setDomainName(rule.healthCheck.httpCheckDomain)
      }
      if (rule.healthCheck.httpCheckPath?.length() > 0) {
        reqBody.setUrlPath(rule.healthCheck.httpCheckPath)
      }
      // set protocol type
      reqBody.setType(CreateHealthmonitorReq.TypeEnum.HTTP)
      body.withHealthmonitor(reqBody)
      req.withBody(body)
      def resp = client.createHealthmonitor(req)
      return resp.getHealthmonitor().getId()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  String deleteLBListenerById(String listenerId) {
    try{
      def req = new DeleteListenerRequest();
      req.setListenerId(listenerId)
      client.deleteListener(req);
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
    return ""
  }

  List<L7policyResp> getAllL7policies(String listenerId) {
    try{
      def req = new ListL7policiesRequest().withListenerId(listenerId)
      def resp = client.listL7policies(req);
      return resp.getL7policies()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<L7ruleResp> getAllL7rules(String policyId) {
    try{
      def req = new ListL7rulesRequest().withL7policyId(policyId)
      def resp = client.listL7rules(req)
      return resp.getRules()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  String createLBListenerRule(String loadBalancerId, String listenerId, HuaweiCloudLoadBalancerRule rule) {
    try{
      def req = new CreateL7policyRequest()
      def body = new CreateL7policyRequestBody()
      def reqBody = new CreateL7policyReq()
      reqBody.setListenerId(listenerId)
      // create pool
      def poolId = createPool(loadBalancerId, "HTTP", "L7")
      if (poolId?.length() > 0) {
        createL7HealthCheck(poolId, rule)
      }
      reqBody.setRedirectPoolId(poolId)
      reqBody.setAction(CreateL7policyReq.ActionEnum.REDIRECT_TO_POOL)

      // set rules
      List<CreateL7ruleReqInPolicy> policies = []

      if (rule.domain?.length() > 0) {
        def reqDomain = new CreateL7ruleReqInPolicy()
        reqDomain.setType(CreateL7ruleReqInPolicy.TypeEnum.HOST_NAME)
        reqDomain.setCompareType("EQUAL_TO")
        reqDomain.setValue(rule.domain)
        policies.add(reqDomain)
      }
      if (rule.url?.length() > 0) {
        def reqUrl = new CreateL7ruleReqInPolicy()
        reqUrl.setType(CreateL7ruleReqInPolicy.TypeEnum.PATH)
        reqUrl.setCompareType("STARTS_WITH")
        reqUrl.setValue(rule.url)
        policies.add(reqUrl)
      }
      if (policies.size() > 0) {
        reqBody.setRules(policies)
      }
      body.withL7policy(reqBody)
      req.withBody(body)
      client.createL7policy(req)
      return "success"
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  List<PoolResp> getAllPools(String loadbalancerId) {
    try{
      def req = new ListPoolsRequest().withLoadbalancerId(loadbalancerId)
      def resp = client.listPools(req)
      return resp.getPools()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  PoolResp getPool(String poolId) {
    try{
      def req = new ShowPoolRequest().withPoolId(poolId)
      def resp = client.showPool(req);
      return resp.getPool()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  HealthmonitorResp getHealthMonitor(String healthId) {
    try{
      def req = new ShowHealthmonitorsRequest().withHealthmonitorId(healthId)
      def resp = client.showHealthmonitors(req);
      return resp.getHealthmonitor()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<MemberResp> getAllMembers(String poolId) {
    try{
      def req = new ListMenbersRequest().withPoolId(poolId)
      def resp = client.listMenbers(req)
      return resp.getMembers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<PoolResp> getAllPools() {
    try{
      def req = new ListPoolsRequest()
      def resp = client.listPools(req)
      return resp.getPools()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  String deRegisterTarget(String poolId, List<HuaweiCloudLoadBalancerTarget> targets) {
    try{
      targets.each {
        def req = new DeleteMemberRequest();
        req.setMemberId(it.instanceId)
        req.setPoolIdId(poolId)
        client.deleteMember(req)
      }
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
    return ""
  }

  String deleteLBListenerRule(HuaweiCloudLoadBalancerRule rule) {
    try{
      def req = new DeleteL7policyRequest();
      req.setL7policyId(rule.policyId)
      def resp = client.deleteL7policy(req);
      if (resp.getHttpStatusCode() == 204) {   //task success
        return "success"
      }
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
    return ""
  }

}
