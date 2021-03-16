package com.netflix.spinnaker.clouddriver.huaweicloud.client

import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerListener
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerRule
import com.netflix.spinnaker.clouddriver.huaweicloud.model.loadbalance.HuaweiCloudLoadBalancerTarget
import com.netflix.spinnaker.clouddriver.huaweicloud.exception.HuaweiCloudOperationException

import com.huaweicloud.sdk.core.auth.BasicCredentials
import com.huaweicloud.sdk.core.exception.ServiceResponseException
import com.huaweicloud.sdk.core.http.HttpConfig
import com.huaweicloud.sdk.core.region.Region
import com.huaweicloud.sdk.elb.v3.ElbClient
import com.huaweicloud.sdk.elb.v3.model.*
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Slf4j
class HuaweiLoadBalancerClient {
  private final DEFAULT_LIMIT = 100
  ElbClient client

  HuaweiLoadBalancerClient(String accessKeyId, String accessSecretKey, String region){
    def auth = new BasicCredentials().withAk(accessKeyId).withSk(accessSecretKey)
    def regionId = new Region(region, "https://elb." + region + ".myhuaweicloud.com")
    def config = HttpConfig.getDefaultHttpConfig()
    client = ElbClient.newBuilder()
        .withHttpConfig(config)
        .withCredential(auth)
        .withRegion(regionId)
        .build()
  }

  List<LoadBalancer> getAllLoadBalancer() {
    List<LoadBalancer> loadBalancerAll = []
    try {
      def req = new ListLoadBalancersRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listLoadBalancers(req)
      def loadBalancers = resp.getLoadbalancers()
      loadBalancerAll.addAll(loadBalancers)
      while (loadBalancers.size() == DEFAULT_LIMIT) {
        req.setMarker(loadBalancers[loadBalancers.size() - 1].getId())
        resp = client.listLoadBalancers(req)
        loadBalancers = resp.getLoadbalancers()
        loadBalancerAll.addAll(loadBalancers)
      }
      return loadBalancerAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<LoadBalancer> getLoadBalancerByName(String name) {
    try{
      def req = new ListLoadBalancersRequest().withName(name)
      def resp = client.listLoadBalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<LoadBalancer> getLoadBalancerById(String id) {
    try{
      def req = new ListLoadBalancersRequest().withId(id)
      def resp = client.listLoadBalancers(req);
      return resp.getLoadbalancers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  String deleteLoadBalancerById(String loadBalancerId) {
    try{
      def req = new DeleteLoadBalancerRequest()
      req.setLoadbalancerId(loadBalancerId)
      client.deleteLoadBalancer(req)
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.toString())
    }
  }

  List<Listener> getAllLBListener(List<String> loadbalancerId) {
    List<Listener> listenerAll = []
    try {
      def req = new ListListenersRequest().withLimit(DEFAULT_LIMIT)
      if (loadbalancerId.size() > 0) {
        req.setLoadbalancerId(loadbalancerId)
      }
      def resp = client.listListeners(req)
      def listeners = resp.getListeners()
      listenerAll.addAll(listeners)
      while (listeners.size() == DEFAULT_LIMIT) {
        req.setMarker(listeners[listeners.size() - 1].getId())
        resp = client.listListeners(req)
        listeners = resp.getListeners()
        listenerAll.addAll(listeners)
      }
      return listenerAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<L7Policy> getAllL7policies(List<String> listenerId) {
    List<L7Policy> l7policiesAll = []
    try {
      def req = new ListL7PoliciesRequest().withLimit(DEFAULT_LIMIT)
      if (listenerId.size() > 0) {
        req.setListenerId(listenerId)
      }
      def resp = client.listL7Policies(req)
      def l7policies = resp.getL7policies()
      l7policiesAll.addAll(l7policies)
      while (l7policies.size() == DEFAULT_LIMIT) {
        req.setMarker(l7policies[l7policies.size() - 1].getId())
        resp = client.listL7Policies(req)
        l7policies = resp.getL7policies()
        l7policiesAll.addAll(l7policies)
      }
      return l7policiesAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<L7Rule> getAllL7rules(String policyId) {
    try{
      def req = new ListL7RulesRequest().withL7policyId(policyId)
      def resp = client.listL7Rules(req)
      return resp.getRules()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Pool> getAllPools(List<String> loadbalancerId) {
    List<Pool> poolsAll = []
    try {
      def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
      if (loadbalancerId.size() > 0) {
        req.setLoadbalancerId(loadbalancerId)
      }
      def resp = client.listPools(req)
      def pools = resp.getPools()
      poolsAll.addAll(pools)
      while (pools.size() == DEFAULT_LIMIT) {
        req.setMarker(pools[pools.size() - 1].getId())
        resp = client.listPools(req)
        pools = resp.getPools()
        poolsAll.addAll(pools)
      }
      return poolsAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  Pool getPool(String poolId) {
    try{
      def req = new ShowPoolRequest().withPoolId(poolId)
      def resp = client.showPool(req);
      return resp.getPool()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  HealthMonitor getHealthMonitor(String healthId) {
    try{
      def req = new ShowHealthMonitorRequest().withHealthmonitorId(healthId)
      def resp = client.showHealthMonitor(req);
      return resp.getHealthmonitor()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<HealthMonitor> getAllHealthMonitors() {
    List<HealthMonitor> healthMonitorsAll = []
    try {
      def req = new ListHealthMonitorsRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listHealthMonitors(req)
      def healthMonitors = resp.getHealthmonitors()
      healthMonitorsAll.addAll(healthMonitors)
      while (healthMonitors.size() == DEFAULT_LIMIT) {
        req.setMarker(healthMonitors[healthMonitors.size() - 1].getId())
        resp = client.listHealthMonitors(req)
        healthMonitors = resp.getHealthmonitors()
        healthMonitorsAll.addAll(healthMonitors)
      }
      return healthMonitorsAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Member> getAllMembers(String poolId) {
    try{
      def req = new ListMembersRequest().withPoolId(poolId)
      def resp = client.listMembers(req)
      return resp.getMembers()
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

  List<Pool> getAllPools() {
    List<Pool> poolsAll = []
    try {
      def req = new ListPoolsRequest().withLimit(DEFAULT_LIMIT)
      def resp = client.listPools(req)
      def pools = resp.getPools()
      poolsAll.addAll(pools)
      while (pools.size() == DEFAULT_LIMIT) {
        req.setMarker(pools[pools.size() - 1].getId())
        resp = client.listPools(req)
        pools = resp.getPools()
        poolsAll.addAll(pools)
      }
      return poolsAll
    } catch (ServiceResponseException e) {
      throw new HuaweiCloudOperationException(e.getErrorMsg())
    }
  }

}
