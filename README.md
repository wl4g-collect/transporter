![mahua](http://mahua.jser.me/mahua-logo.jpg)
## Transporter(分布式集群推送服务)：

##### (1) 模块说明：
          transport-server-common    -->    公共模块
             transport-server-api    -->    多模块依赖调用接口定义
         transport-server-cluster    -->    Cluster(Akka/Actor)集群通信模块       
            transport-server-rest    -->    Restful接口模块
            transport-server-core    -->    核心公共RPC(Netty)传输模块
            transport-server-data    -->    数据接口Service(hbase)模块
           transport-server-kafka    -->    MQ(Kafka)实现模块
         transport-server-starter    -->    Transporter服务启动模块
             transport-client-sdk    -->    客户端SDK独立模块


##### (2) 部署说明：
    <a> 安装 transporter/pom.xml clean install -DskipTests
    <b> 服务管理:
      b1. 启动Transport服务 [root@192.168.0.100 ~] $TRANSPORT_HOME/bin/transport-start.sh
      b2. 停止Transport服务 [root@192.168.0.100 ~] $TRANSPORT_HOME/bin/transport-stop.sh
	<c> 配置(含集群)说明:
      c1. 参见示例配置文件：conf/application-node.yml
    <d>. 负载均衡原理：
         已实现类redis集群去中心的负载均衡策略, client --crc16(deviceId)--> server cluster(node1/node2/node3...);
![mahua](https://github.com/wang4ever/transporter/blob/master/docs/Transporter(分布式推送系统架构图).png)


##### (3) 性能调优：
*	&lt;a&gt; 推荐生产环境: 参见文档：etc/Tuning Guide.txt

##### (4) 各客戶端认证原理（伪代码）：
    <a> 定义说明:
        a1. 客户端：使用transport-client-sdk-xxx.jar 表示是第三方系统后台或第三方系统Android端;
        a2. Web(WebSocket)端：使用transport-ws-vxxx.jar 表示是第三方系统的Web端;
    <b> 客户端认证流程:
      step1: appId、appSecret、groupId、deviceId、deviceType -> jedis.get(appId) == appSecret
          说明: 客户端调用TransportClients.getInstance().build(..) 新建channel(netty)连接之后，sdk自动携带
          appId、appSecret、groupId、deviceId、deviceType 向Transport-server发起认证请求，服务端将验证 
          jedis.get(appId) == appSecret 是否成立，然后返回认证结果;
      step2: deviceIds -> jedis.set(deviceId, appId)
          说明: 第三方系统的登录完成后主动回调sdk的client.registered() 注册登录用户deviceId(token)，用于后续
          新建WebSocket连接后认证使用的deviceId，此过程sdk将自动携带appId、deviceIds发送给服务端，服务端收到后
          进入注册流程，即：for(deviceIds){ jedis.set(deviceId, appId); }
      step3: 待以上step1、step2完成之后，即可进入下面的WebSocket连接认证流程;
    <c> WebSocket证流程:
        appId、groupId、deviceId、deviceType -> jedis.get(deviceId) == appId
        说明: Web调用window.Transporter.init(..)新建websocket连接后，插件自动携带appId、groupId、deviceId、deviceType
        向Transport-server发起认证请求，服务端将验证 jedis.get(deviceId) == appId 是否成立，然后返回认证结果;

##### (5) 推送规则说明：
*	&lt;a&gt; sdk单播推送：```client.unicast(toDeviceId, payload);``` // 只推送当前连接的toDeviceId设备
*	&lt;b&gt; sdk组播推送：```client.groupcast(toGroupId, payload);``` // 会推送当前连接的属于toGroupId所有设备

##### (6) SDK集成说明：
    <a> Web端初始化时groupId建议使用第三方系统的userId（因为userId与deviceId可能one-to-many的关系, 这样组播时
        会将所有userId的设备都推送到(解决多点登录问题)，酱紫是最优的方式）
    <b> 客户端初始化时groupId可自行设置.（限制条件：groupId(max)=32）

##### (7) SDK开发示例：
Example:
```java
import com.alibaba.fastjson.JSON;
import io.netty.util.CharsetUtil;
import io.transport.sdk.protocol.handler.ReceiveTextHandler;
import io.transport.sdk.protocol.message.internal.ResultRespMessage;
import io.transport.sdk.protocol.message.internal.TransportMessage;
import io.transport.sdk.Configuration;
import io.transport.sdk.TransportClients;
import io.transport.sdk.protocol.message.DeviceInfo.DeviceType;

public class MyTransportProgram {
  public static void main(String[] args) throws Exception {
    // 1.0 Create config object.
    Configuration config = new Configuration(appId, appSecret, groupId, MyReceiveTextHandler.class, new Store() {
        
        @Override
        public void put(String key, String value) {
            // TODO Auto-generated method stub
            globalCache.put(key, value);
        }
        
        @Override
        public String get(String key) {
            // TODO Auto-generated method stub
            return globalCache.get(key);
        }
        
        @Override
        public void remove(String key) {
            // TODO Auto-generated method stub
            globalCache.remove(key);
        }
	});
	config.setHostAndPorts("127.0.0.1:10030");

    // 2.0 Create Transport instance and connect.
    TransportClients client = TransportClients.getInstance().build(config).join();
    // build.destroy(); // Destroy Transport instance.

    // 3.0 Registered WebSocket(browser) authentication device.
    //client.registered(24 * 60 * 60, "b0898e70d0d64a77bf51b798466e85e3");

    // 4.0 Broadcast messages to device.
    String toDeviceId = "b0898e70d0d64a77bf51b798466e85e3";
    String toGroupId = "ae10f490378f";
    String payload = "{\"testKey\":\"testValue\", \"TestData\":\"Test the message sent to the browser.\"}";
    // 4.1 Unicast broadcasting.
    //client.unicast(toDeviceId, payload);
    // 4.2 Groupcast broadcasting.
    client.groupcast(toGroupId, payload);
  }
  
  public static class MyReceiveTextHandler extends ReceiveTextHandler {

    @Override
    protected void onConnected(String deviceIdToken) {
      System.out.println("认证成功... deviceIdToken=" + deviceIdToken);
    }

    @Override
    protected void onMessage(TransportMessage msg) {
      // TODO Auto-generated method stub
      System.out.println("Transport服务器返回消息：" + JSON.toJSONString(msg));
      //
      // 从这里开始你收到消息后的逻辑...
      //
      
    }

    @Override
    protected void onResult(ResultRespMessage msg) {
      // TODO Auto-generated method stub
      System.out.println("Transport服务器返回处理结果消息：" + JSON.toJSONString(msg));
    }
  }

}
```


##### (8) 监控仪表盘：
	<a> 需首先配置仪表盘授权白名单及账号.
```
rest:
  auth:
    allow-ip: 127.0.0.1,192.168.212.0/24
    disable-ip:
    users: admin:admin,guest:123456
```
	<b> 实时监控接口：http://127.0.0.1:8080/api/v1/monitor?username=guest&password=123456
