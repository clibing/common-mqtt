package cn.linuxcrypt.common.exception;

/**
 * @author clibing
 */
public class MqttException extends RuntimeException {
    public MqttException() {
    }

    public MqttException(String message) {
        super(message);
    }

    public MqttException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttException(Throwable cause) {
        super(cause);
    }
}
