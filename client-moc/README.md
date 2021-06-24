## Preparing
```shell script
mvn io.quarkus:quarkus-maven-plugin:1.11.6.Final:create -DprojectGroupId=com.redhat.qws -DprojectArtifactId=client-moc -DclassName=com.redhat.qws.client.ClientResource -Dpath=/client -Dextensions="rest-client,resteasy,resteasy-jsonb,openshift,scheduler,websockets"
```

### If miss to add extensions
```shell script
mvn quarkus:add-extension -Dextensions="resteasy-jsonb"
```

### Running the app for specific profile
```shell script
java -Dquarkus.profile=local -jar target/client-moc-1.0.0-SNAPSHOT-runner.jar
```

### Run the app with custom trusted store
At the moment OpenSSL does not generate corect PCSK12 bundle for JAVA trusted store
```shell script
java "-Djavax.net.debug=all" "-Djavax.net.ssl.trustStore=$(pwd)/cert/trustStore.jks" "-Djavax.net.ssl.trustStorePassword=1234567890" '-Dquarkus.log.category.\"com.redhat.qws\".level=WARN' -jar target/client-moc-1.0.0-SNAPSHOT-runner.jar
mvn "-Djavax.net.debug=none" "-Djavax.net.ssl.trustStore=$(pwd)/cert/trustStore.jks" "-Djavax.net.ssl.trustStorePassword=1234567890" '-Dquarkus.log.category.\"com.redhat.qws\".level=WARN' "-Dquarkus.kubernetes.deploy=true" "-DskipTests" clean package
```

### Prepare OpenShift objects
```
oc new-build --name java-11-base -D - < src/main/docker/Dockerfile.base
oc create secret generic client-tls-config --from-file=ca.crt=cert/ca.crt --from-file=trustStore.jks=cert/trustStore.jks --from-file=clientr-moc.pfx=cert/client-moc.pfx --from-file=client-moc.crt=cert/client-moc.crt --from-file=client-moc.key=cert/client-moc.key
oc create cm client-app-properties --from-file=application.properties=src/main/resources/application.properties
```
