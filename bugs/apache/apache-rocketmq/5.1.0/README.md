## CVE-2023-37582

### 漏洞名称：Apache RocketMQ 更新配置远程代码执行漏洞

### 漏洞描述
在RocketMQ 5.1.1及以上版本中，当RocketMQ的NameServer暴露在外网且缺乏权限验证时，
恶意攻击者可以通过未授权访问利用更新配置功能以RocketMQ运行的系统用户身份上传含有特定
内容的文件到任意目录。通过对Linux系统写入crontab的方式即可实现任意命令执行。

### sink方法
```yml
{ method: "<org.apache.rocketmq.common.utils.IOTinyUtils: void writeStringToFile(java.io.File,java.lang.String,java.lang.String)>", index: 1 }
```

### source方法
```yml
   { kind: param, method: "<org.apache.rocketmq.namesrv.processor.DefaultRequestProcessor: org.apache.rocketmq.remoting.protocol.RemotingCommand processRequest(io.netty.channel.ChannelHandlerContext,org.apache.rocketmq.remoting.protocol.RemotingCommand)>", index: 1 }
```

### 需推测的transfer
```yml
"1"{ method: "<org.apache.rocketmq.remoting.Configuration: void mergeIfExist(java.util.Properties,java.util.Properties)>", from: 0, to: 1 }
"2"{ method: "<org.apache.rocketmq.remoting.protocol.RemotingCommand: byte[] getBody()>", from: base, to: result }
"3"{ method: "<org.apache.rocketmq.common.MixAll: java.util.Properties string2Properties(java.lang.String)>", from: 0, to: result }
"4"{ method: "<java.lang.String: void <init>(byte[],java.lang.String)>", from: 0, to: base, type: java.lang.String }
"5"{ method: "<org.apache.rocketmq.common.MixAll: java.lang.String properties2String(java.util.Properties,boolean)>", from: 0, to: result, type: java.lang.String }
"6"{ method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", from: 0, to: base, type: java.lang.StringBuilder }
"7"{ method: "<java.lang.StringBuilder: java.lang.String toString()>", from: base, to: result, type: java.lang.String }
```

### 评估结果

### 污点路径

每一行: 

(class name): (method signature) (taint variable/field name)

tmp+数字：没有变量名的临时变量，源代码中不存在，路径中临时添加

```
Server: main(String[] args) remotingCommand ->
DefaultRequestProcessor: processRequest(ChannelHandlerContext ctx, RemotingCommand request) request ->
DefaultRequestProcessor: updateConfig(ChannelHandlerContext ctx, RemotingCommand request) request --transfer2-->
DefaultRequestProcessor: updateConfig(ChannelHandlerContext ctx, RemotingCommand request) body --transfer4-->
DefaultRequestProcessor: updateConfig(ChannelHandlerContext ctx, RemotingCommand request) bodyStr --transfer3-->
DefaultRequestProcessor: updateConfig(ChannelHandlerContext ctx, RemotingCommand request) properties ->
Configuration: update(Properties properties) properties --transfer1-->
Configuration: update(Properties properties) this.allConfigs --transfer5-->
Configuration: getAllConfigsInternal() temp1 --transfer6-->
Configuration: getAllConfigsInternal() stringBuilder ->
Configuration: persist() allConfigs ->
MixAll: string2File(String str, String fileName) str ->
MixAll: string2FileNotSafe(String str, String fileName) str ->
IOTinyUtils: writeStringToFile(File file, String data, String encoding) data
```