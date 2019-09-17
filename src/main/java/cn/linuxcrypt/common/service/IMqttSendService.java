package cn.linuxcrypt.common.service;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 定义接口，用于发送消息，实现类有spring代理
 */
@Component
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface IMqttSendService {

    /**
     * 发送MQTT消息
     * @param data
     */
    void sendToMqtt(String data);

    /**
     * 发送MQTT消息
     * @param topic
     * @param payload
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String payload);

    /**
     * 发送MQTT消息
     * @param topic
     * @param qos
     * @param payload
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Header(MqttHeaders.QOS) int qos, String payload);

}
