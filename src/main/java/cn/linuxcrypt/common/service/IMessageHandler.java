package cn.linuxcrypt.common.service;

/**
 * @function:
 * @author: liuboxun
 * @email: wmsjhappy@gmail.com
 * @date: 2019/9/17
 * @remark:
 * @version: 1.0
 */
public interface IMessageHandler {
    /**
     * 向话题发送消息，data为json格式
     *
     * @param topic
     * @param jsonData
     */
    void doHandler(String topic, String jsonData);
}
