Spinnaker Cloud Provider Service
------------------------------------
[![Build Status](https://api.travis-ci.org/spinnaker/clouddriver.svg?branch=master)](https://travis-ci.org/spinnaker/clouddriver)

This service is the main integration point for Spinnaker cloud providers like AWS, GCE, CloudFoundry, Azure etc. 

### Developing with Intellij

To configure this repo as an Intellij project, run `./gradlew idea` in the root directory. 

Some of the modules make use of [Lombok](https://projectlombok.org/), which will compile correctly on its own. However, for Intellij to make sense of the Lombok annotations, you'll need to install the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok-plugin) as well as [check 'enable' under annotation processing](https://www.jetbrains.com/help/idea/configuring-annotation-processing.html#3).

### Debugging

To start the JVM in debug mode, set the Java system property `DEBUG=true`:
```
./gradlew -DDEBUG=true
```

The JVM will then listen for a debugger to be attached on port 7102.  The JVM will _not_ wait for
the debugger to be attached before starting Clouddriver; the relevant JVM arguments can be seen and
modified as needed in `build.gradle`.

### Added by 叶静涛

修复了spinnaker对接AWS中国区不可用的几个问题

问题1：竟价地址请求错误

    com.netflix.spinnaker.clouddriver.aws.provider.agent.AmazonInstanceTypeCachingAgent
	
问题2：没有根据区域设置合适的endpoint，默认的sts.amazonaws.com无法认证cn-north-1

    com.netflix.spinnaker.clouddriver.aws.security.NetflixSTSAssumeRoleSessionCredentialsProvider
	
	PS：目前先硬编码为北京1区，后续做优化
	
问题3：assumeRole全称的生成未考虑中国区的特殊性，以致中国区的assumeRole全称错误

    解决方案：assumeRole必须使用全称，即arn:aws-cn:iam::048625849086:role/spinnakerManaged

源码启动方式：
```
nohup sh -c "./gradlew --daemon 2>&1 | tee /tmp//clouddriver.log | cat >/dev/null" >/dev/null &
```

如果需要jvm远程调试（调试端口7102）：
```
nohup sh -c "./gradlew -DDEBUG=true --daemon 2>&1 | tee /tmp//clouddriver.log | cat >/dev/null" >/dev/null &
```

