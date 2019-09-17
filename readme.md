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