package cn.linuxcrypt.common.config;

import cn.linuxcrypt.common.TopicEnum;
import cn.linuxcrypt.common.service.IMessageHandler;
import cn.linuxcrypt.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

import javax.annotation.Resource;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * MQTT接收消息处理
 *
 * @author Administrator
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@IntegrationComponentScan({"cn.linuxcrypt.common.config", "cn.linuxcrypt.common.service"})
public class MqttReceiveConfig {

    @Autowired
    private MqttProperties mqttProperties;

    @Resource
    private IMessageHandler messageHandler;

    @Bean
    public MqttConnectOptions getMqttConnectOptions(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，
        // 这里设置为true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(mqttProperties.getCleanSession());

        // 设置超时时间 单位为秒
        mqttConnectOptions.setConnectionTimeout(mqttProperties.getConnectionTimeoutSecond());

        if (StringUtils.isNotBlank(mqttProperties.getUsername())) {
            mqttConnectOptions.setUserName(mqttProperties.getUsername());
            mqttConnectOptions.setPassword(Optional.ofNullable(mqttProperties.getPassword()).orElse("").toCharArray());
        }
        mqttConnectOptions.setServerURIs(Optional.ofNullable(mqttProperties.getHostUrls()).get());

        // 设置会话心跳时间
        // 例如 值为20时， 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，
        // 但这个方法并没有重连的机制
        mqttConnectOptions.setKeepAliveInterval(mqttProperties.getKeepAliveIntervalSecond());

        // 是否使用ssl
        if(Boolean.TRUE.equals(mqttProperties.getSsl())){
            SSLSocketFactory socketFactory = null;
            try {
                socketFactory = getSocketFactory(mqttProperties.getRootCa(),
                        mqttProperties.getClientCa(), mqttProperties.getClientKey(), "");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mqttConnectOptions.setSocketFactory(socketFactory);
        }
        return mqttConnectOptions;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getMqttConnectOptions());
        return factory;
    }

    /**
     * 接收通道
     *
     * @return
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * 发送通道
     *
     * @return
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * 配置client,监听的topic
     *
     * @return
     */
    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = null;
        if(StringUtils.isBlank(mqttProperties.getDefaultTopic())){
            adapter = new MqttPahoMessageDrivenChannelAdapter(mqttProperties.getClientId() + "_inbound",
                    mqttClientFactory(), TopicEnum.OFFLINE.value, TopicEnum.ONLINE.value, TopicEnum.SYSTEM.value);
        }else{
            adapter = new MqttPahoMessageDrivenChannelAdapter(mqttProperties.getClientId() + "_inbound",
                    mqttClientFactory(), TopicEnum.OFFLINE.value, TopicEnum.ONLINE.value, TopicEnum.SYSTEM.value,
                    mqttProperties.getDefaultTopic());
        }
        adapter.setCompletionTimeout(mqttProperties.getCompletionTimeoutMillis());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttProperties.getQos());
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * 通过通道获取数据
     *
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public org.springframework.messaging.MessageHandler handler() {
        return message -> {
            log.debug("message json:{}", JsonUtil.toJson(message));
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
            messageHandler.doHandler(topic, message.getPayload().toString());
        };
    }


    /**
     * 发送消息配置
     *
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public org.springframework.messaging.MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(mqttProperties.getClientId(), mqttClientFactory());
        messageHandler.setAsync(mqttProperties.getSendAsync());
        messageHandler.setDefaultTopic(mqttProperties.getDefaultTopic());
        return messageHandler;
    }

    private static SSLSocketFactory getSocketFactory(final String caCrtFile,
                                                     final String crtFile, final String keyFile, final String password)
            throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // load CA certificate
        X509Certificate caCert = null;

//        FileInputStream fis = new FileInputStream(caCrtFile);
        InputStream rootCaStream = MqttReceiveConfig.class.getResourceAsStream(caCrtFile);
        BufferedInputStream bis = new BufferedInputStream(rootCaStream);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        while (bis.available() > 0) {
            caCert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client certificate
        bis = new BufferedInputStream(MqttReceiveConfig.class.getResourceAsStream(crtFile));
        X509Certificate cert = null;
        while (bis.available() > 0) {
            cert = (X509Certificate) cf.generateCertificate(bis);
            // System.out.println(caCert.toString());
        }

        // load client private key
        InputStream keyStream = MqttReceiveConfig.class.getResourceAsStream(keyFile);
        PEMParser pemParser = new PEMParser(new InputStreamReader(keyStream));
        Object object = pemParser.readObject();
        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                .build(password.toCharArray());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                .setProvider("BC");
        KeyPair key;
        if (object instanceof PEMEncryptedKeyPair) {
            System.out.println("Encrypted key - we will use provided password");
            key = converter.getKeyPair(((PEMEncryptedKeyPair) object)
                    .decryptKeyPair(decProv));
        } else {
            System.out.println("Unencrypted key - no password needed");
            key = converter.getKeyPair((PEMKeyPair) object);
        }
        pemParser.close();

        // CA certificate is used to authenticate server
        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate
        // us
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[] { cert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }
//    //通过通道获取数据
//    @Bean
//    @ServiceActivator(inputChannel = "mqttInputChannel")
//    public MessageHandler handler() {
//        return new MessageHandler() {
//            @Override
//            public void handleMessage(Message<?> message) throws MessagingException {
//                String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
//                String type = topic.substring(topic.lastIndexOf("/")+1, topic.length());
//                mqttReceiveService.handlerMqttMessage(topic,message.getPayload().toString());
//            }
//        };
//    }

//    // 多client
//
//    //通道2
//    @Bean
//    public MessageChannel mqttInputChannelTwo() {
//        return new DirectChannel();
//    }
//    //配置client2，监听的topic:hell2,hello3
//    @Bean
//    public MessageProducer inbound1() {
//        MqttPahoMessageDrivenChannelAdapter adapter =
//                new MqttPahoMessageDrivenChannelAdapter(clientId+"_inboundTwo", mqttClientFactory(),
//                        "hello2","hello3");
//        adapter.setCompletionTimeout(completionTimeout);
//        adapter.setConverter(new DefaultPahoMessageConverter());
//        adapter.setQos(1);
//        adapter.setOutputChannel(mqttInputChannelTwo());
//        return adapter;
//    }
//
//    //通过通道2获取数据
//    @Bean
//    @ServiceActivator(inputChannel = "mqttInputChannelTwo")
//    public MessageHandler handlerTwo() {
//        return new MessageHandler() {
//            @Override
//            public void handleMessage(Message<?> message) throws MessagingException {
//                String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
//                String type = topic.substring(topic.lastIndexOf("/")+1, topic.length());
//                if("hello2".equalsIgnoreCase(topic)){
//                    System.out.println("hello2 clientTwo,"+message.getPayload().toString());
//                }else if("hello3".equalsIgnoreCase(topic)){
//                    System.out.println("hello3 clientTwo,"+message.getPayload().toString());
//                }
//            }
//        };
//    }
}
