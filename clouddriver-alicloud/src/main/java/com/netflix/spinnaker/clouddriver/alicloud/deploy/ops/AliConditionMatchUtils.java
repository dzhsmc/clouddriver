package com.netflix.spinnaker.clouddriver.alicloud.deploy.ops;

import com.aliyuncs.ess.model.v20140828.DescribeScalingGroupsResponse;
import org.springframework.util.StringUtils;

/**
 * @author chen_muyi
 * @date 2021/2/24 14:30
 */
public class AliConditionMatchUtils {

  public static boolean match(
      String conditionString, DescribeScalingGroupsResponse.ScalingGroup target) {
    if (StringUtils.isEmpty(conditionString)) {
      return true;
    }
    return target.getScalingGroupName().matches(conditionString + ".*");
  }
}
