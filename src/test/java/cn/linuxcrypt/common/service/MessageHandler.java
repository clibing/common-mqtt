package cn.linuxcrypt.common.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @function:
 * @author: liuboxun
 * @email: wmsjhappy@gmail.com
 * @date: 2019/9/17
 * @remark:
 * @version: 1.0
 */
@Slf4j
@Service
public class MessageHandler implements IMessageHandler {

    @Override
    public void doHandler(String topic, String jsonData) {
        log.info("topic: {}, json data: {}", topic, jsonData);
    }
}
