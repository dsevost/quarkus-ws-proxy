## Preparing 
```shell script
mvn io.quarkus:quarkus-maven-plugin:1.11.6.Final:create -DprojectGroupId=com.redhat.qws -DprojectArtifactId=ws-server -DclassName=com.redhat.qws.proxy.WebSocketServerResource -Dpath=/proxy -Dextensions="grpc,infinispan-client,micrometer-registry-prometheus,rest-client,resteasy,resteasy-jsonb,scheduler,websockets"
```
### If miss to add extensions
```shell script
mvn quarkus:add-extension -Dextensions="resteasy-jsonb"
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

### Run the app with custom trusted store
At the moment OpenSSL does not generate corect PCSK12 bundle for JAVA trusted store
```shell script
java "-Djavax.net.debug=all" "-Djavax.net.ssl.trustStore=$(pwd)/cert/trustStore.jks" "-Djavax.net.ssl.trustStorePassword=1234567890" "-Dquarkus.grpc.server.port=9000" "-Dquarkus.grpc.clients.smartclient.port=9000" "-Dquarkus.grpc.server.port=9100" '-Dquarkus.log.category.\"com.redhat.qws\".level=WARN' -jar target/ws-server-1.0.0-SNAPSHOT-runner.jar
```

### Prepare OpenShift objects
```
oc create secret generic ws-server-tls-config --from-file=ca.crt=cert/ca.crt --from-file=trustStore.jks=cert/trustStore.jks --from-file=ws-server.pfx=cert/ws-server.pfx --from-file=ws-server.crt=cert/ws-server.crt --from-file=ws-server.key=cert/ws-server.key
oc create cm ws-server-app-properties --from-file=application.properties=src/main/resources/application.properties
```

```
mvn "-DskipTests=true" "-Dquarkus.kubernetes.deploy=true" package
```