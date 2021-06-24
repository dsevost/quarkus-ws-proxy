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
