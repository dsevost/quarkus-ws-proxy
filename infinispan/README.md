## Managing local Infinispan server
### CLI tool 
```shell script
bin/cli.sh config set truststore ../cert/trustStore.jks       
bin/cli.sh config set truststore-password 1234567890   
bin/cli.sh user create admin -g admin -p admin 
bin/cli.sh user create developer -g deployer -p developer
```
### Midify local server configuration
server/conf/infinispan.xml (xpath: /infinispan/server/security/security-realms/security-realm[@name=default])
```xml
               <server-identities>
                  <ssl>
                     <keystore path="../../../cert/datagrid.pfx" relative-to="infinispan.server.config.path"
                               keystore-password="1234567890" alias="datagrid" key-password="1234567890"/>
                     <!-- enable mutual TLS -->
                     <!-- truststore path="trust.p12"
                           relative-to="infinispan.server.config.path"
                           password="secret"/ -->
                  </ssl>
               </server-identities>
```
server/conf/infinispan.xml (xpath: /infinispan/server/security/endpoints[@socket-binding=default and @security-realm="default"])
```xml
          <hotrod-connector>
              <authentication>
                  <sasl mechanisms="SCRAM-SHA-512 SCRAM-SHA-384 SCRAM-SHA-256
                           SCRAM-SHA-1 DIGEST-SHA-512 DIGEST-SHA-384
                           DIGEST-SHA-256 DIGEST-SHA DIGEST-MD5 PLAIN"
                  />
              </authentication>
          </hotrod-connector>
          <rest-connector>
              <authentication mechanisms="BASIC DIGEST"/>
          </rest-connector>
```
### Using simple server configuration XML
Copy infinispan-localhost-simple-config.xml to server/conf/infinispan.xml of Infinispan server instance
```
cp manifest/infinispan-localhost-simple-config.xml $INFINISPAN_HOME/server/conf/infinispan.xml
```
## Operator managed Datagrid
### Clone openshift-day2 repo
```shell script
git clone https://github.com/rhte-ru/openshift4-day2 ocp
```

### Namespace preparation
```shell script
export NS=ds-quarkus-ws-dev

export OPERATOR_NAME=datagrid
export LABEL_KEY=operator.name
export LABEL_VALUE=${OPERATOR_NAME}

# create namespace if needed
#bash ocp/base/bin/002-namespace-create.sh
```

### Manage OLM Subscription
```shell script
bash ocp/operator/bin/110-operatorgroup-create.sh

export OPERATOR_CHANNEL=8.2.x
export OPERATOR_INSTANCE=datagrid-ws

bash ocp/operator/bin/120-operator-subscription.sh
```

### Doploy Datagrid instance
```shell script
bash ocp/operator/infinispan/bin/210-datagrid-identities.sh

export KEYSTORE_PASSWORD=1234567890

bash ocp/operator/infinispan/bin/220-datagrid-tls.sh

bash ocp/operator/infinispan/bin/230-datagrid-deploy.sh ocp/operator/infinispan/manifest/datagrid-service.yaml
```
