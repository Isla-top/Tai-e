## 污点打在request

#### 传播路径

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

#### 需要的transfer

```yml
  { method: "<org.apache.rocketmq.remoting.Configuration: void mergeIfExist(java.util.Properties,java.util.Properties)>", from: 0, to: 1 }
  { method: "<org.apache.rocketmq.remoting.protocol.RemotingCommand: byte[] getBody()>", from: base, to: result }
  { method: "<org.apache.rocketmq.common.MixAll: java.util.Properties string2Properties(java.lang.String)>", from: 0, to: result }
  { method: "<java.lang.String: void <init>(byte[],java.lang.String)>", from: 0, to: base, type: java.lang.String }
  { method: "<org.apache.rocketmq.common.MixAll: java.lang.String properties2String(java.util.Properties,boolean)>", from: 0, to: result, type: java.lang.String }
  { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", from: 0, to: base, type: java.lang.StringBuilder }
  { method: "<java.lang.StringBuilder: java.lang.String toString()>", from: base, to: result, type: java.lang.String }
```

## 污点打在request内部存储的String

#### 传播路径

第一段污点传播
```
```

#### 需要的transfer

```yml
```
