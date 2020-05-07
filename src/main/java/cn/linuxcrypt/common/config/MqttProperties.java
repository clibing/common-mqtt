package cn.linuxcrypt.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author clibing
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "common.mqtt")
public class MqttProperties {
    /**
     * 接入账号
     */
    private String username;

    /**
     * 接入密码
     */
    private String password;

    /**
     * mqtt服务器URL
     * 例如：tcp://localhost:1883
     */
    private String[] hostUrls;

    /**
     * 客户端唯一标识
     */
    private String clientId;

    /**
     * 系统默认的话题
     */
    private String defaultTopic;

    /**
     * 设置会话心跳时间
     * 例如 值为20时， 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，
     */
    private Integer keepAliveIntervalSecond = 20;

    /**
     * 设置超时时间 单位为秒
     */
    private Integer connectionTimeoutSecond = 10;

    /**
     * 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
     * 这里设置为true表示每次连接到服务器都以新的身份连接
     */
    private Boolean cleanSession = true;

    /**
     * 超时时间
     */
    private Long completionTimeoutMillis = 5000L;

    /**
     * 默认的消息服务质量
     * 0: 发送者只发送一次消息，不进行重试，Broker不会返回确认消息
     * 1: 发送者最少发送一次消息，确保消息到达Broker，Broker需要返回确认消息PUBACK
     * 2: Qos2使用两阶段确认来保证消息的不丢失和不重复
     * <p>
     * 与Qos1相比，Qos2的开销会很大，因为Broker有额外的两个动作：
     * * 去重。为了保证消息不重复，Broker必须进行去重处理；
     * * 确保响应消息PUBREC到达客户端。Broker必须等待客户端对PUBREC消息的响应PUBREL，只有收到客户端的确认消息后，Broker才能对订阅者投递消息。
     */
    private Integer qos = 1;

    /**
     * 是否异步发送
     */
    private Boolean sendAsync = true;

    /**
     * 是否开启ssl
     */
    private Boolean ssl = false;

    /**
     * 根证书
     * classpath:
     */
    private String rootCa = "/root-ca.pem";

    /**
     * 客户端pem
     * classpath:
     */
    private String clientCa = "/client.csr";

    /**
     * 客户端key
     */
    private String clientKey = "/client.key";
}
