## Preparing 
```shell script
mvn io.quarkus:quarkus-maven-plugin:1.11.6.Final:create -DprojectGroupId=com.redhat.qws -DprojectArtifactId=sender-moc -DclassName=com.redhat.qws.sender.SenderResource -Dpath=/mgmt -Dextensions="grpc,micrometer-registry-prometheus,rest-cleint,resteasy,resteasy-jsonb,scheduler"
```
### If miss to add extensions
```shell script
mvn quarkus:add-extension -Dextensions="resteasy-jsonb"
```

### Running the app for specific profile
```shell script
java -Dquarkus.profile=local -jar target/sender-moc-1.0.0-SNAPSHOT-runner.jar
```

### Run the app with custom trusted store
At the moment OpenSSL does not generate corect PCSK12 bundle for JAVA trusted store
```shell script
java "-Djavax.net.debug=all" "-Djavax.net.ssl.trustStore=$(pwd)/cert/trustStore.jks" "-Djavax.net.ssl.trustStorePassword=1234567890" "-Dquarkus.grpc.server.port=9000" "-Dquarkus.grpc.clients.ws-proxy.port=9100" '-Dquarkus.log.category.\"com.redhat.qws\".level=WARN' -jar target/sender-moc-1.0.0-SNAPSHOT-runner.jar
```

### Test gRPC Service
```shell script
grpcurl -insecure -v -d "{ \"client\": { \"clientId\": \"cid-1\", \"user\": { \"name\": \"u-1\" } }, \"ip\": \"1.2.3.4\" }" localhost:9000 sender.SmartClient/unsubscribe
grpcurl -insecure -v -d @ localhost:9000 sender.SmartClient/unsubscribe < smartclientcontext.json
```

### Prepare OpenShift objects
```
oc new-build --name java-11-base -D - < src/main/docker/Dockerfile.base
oc create secret generic sender-tls-config --from-file=ca.crt=cert/ca.crt --from-file=trustStore.jks=cert/trustStore.jks --from-file=sender-moc.pfx=cert/sender-moc.pfx --from-file=sender-moc.crt=cert/sender-moc.crt --from-file=sender-moc.key=cert/sender-moc.key
oc create cm sender-app-properties --from-file=application.properties=src/main/resources/application.properties
```
