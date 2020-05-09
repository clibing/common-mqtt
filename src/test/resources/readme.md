When openssl generates the root ca, it does not specify CN, just specify /C/ST/L/O/OU, and specify /CN when signing, which is also the link when you access MQTT Broker

For example, /CN=clibing.com, MQTT access when the URL is mqtts://clibing.com:port

example: 

### Run CA env
```
docker run -it --name centos7 -v `pwd`/conf:/conf centos:centos7 /bin/sh

cd /conf/

cp /etc/pki/tls/openssl.cnf ./

rm -rf /etc/pki/CA/*.old

touch /etc/pki/CA/index.txt

echo 01 > /etc/pki/CA/serial

rm -rf certs;mkdir certs
```

### create base ca

```
openssl genrsa -out certs/root-ca.key 2048

openssl req -new -x509 -days 36500 -config ./openssl.cnf -key certs/root-ca.key -out certs/root-cacert.pem -subj "/C=CN/ST=Beijing/O=clibing/OU=clibing.com"

```

/C=CN/ST=Beijing/O=clibing/OU=`clibing.com`, the `OU` not `CN`

### check ca 

```
openssl x509 -in certs/root-cacert.pem -noout -text
```

### create server pem

```
openssl genrsa -out certs/server.key 2048

openssl req -new -days 3650 -key certs/server.key -out certs/server-cert.csr -subj "/C=CN/ST=Beijing/O=clibing/OU=clibing.com/CN=clibing.com"

openssl ca -config ./openssl.cnf -extensions v3_req -days 3650 -in certs/server-cert.csr -out certs/server-cert.pem -cert certs/root-cacert.pem -keyfile certs/root-ca.key
```

### verfiy

```
openssl verify -CAfile certs/root-cacert.pem certs/server-cert.pem
```

copy server-cert.pem server-key.

### emqx config 

```
listener.ssl.external.keyfile = etc/certs/server.key
listener.ssl.external.certfile = etc/certs/server-cert.pem 

```

### last replace ESP8266_RTOS_SDK/example/.../ssl demo pem is test ok