##### MQTT模块

* 最简使用

````yaml
common:
  mqtt:
    client-id: ${spring.application.name}
    host-urls:
      - "tcp://172.16.11.199:1883"
    username: ${spring.application.name}
    password:
    ...
````

* 实现`cn.linuxcrypt.common.service.IMessageHandler`中的doHandler方法，用于处理接收到的消息

对消息的验证是否合法性。签名验证等操作

* 注入发送消息服务，即可发送消息

```java
public class MessageService {
    @Resource
    private IMqttSendService mqttSendService;

    /**
     * 发送
     * @param topicName
     * @param data
     * @return
     */
    public Result<Boolean> sendMessage(String topicName, Object data) {
        String payload = JsonUtil.toJson(data);
        log.debug("向话题: {}, 设置发送指令(payload): {}", topicName, payload);
        mqttSendService.sendToMqtt(topicName, payload);
        return Result.success("指令已经下发");
    }
}
```