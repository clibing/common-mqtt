package cn.linuxcrypt.common;

import lombok.Getter;

public enum TopicEnum {
    ONLINE("/Online", "上线"),
    OFFLINE("/Offline", "离线"),
    SYSTEM("/System", "系统"),

    DEVICE("/topic/%s", "设备私有话题，chipid");

    public String value;
    @Getter
    private String title;

    TopicEnum(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public static TopicEnum defaultTopic(String topic) {
        for (TopicEnum t : TopicEnum.values()) {
            if (t.value.equals(topic)) {
                return t;
            }
        }
        return null;
    }
}