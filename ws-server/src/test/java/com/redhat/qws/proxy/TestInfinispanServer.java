package com.redhat.qws.proxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.globalstate.impl.VolatileLocalConfigurationStorage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.server.core.admin.embeddedserver.EmbeddedServerAdminOperationHandler;
import org.infinispan.server.core.security.simple.SimpleServerAuthenticationProvider;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class TestInfinispanServer implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(TestInfinispanServer.class);

    String authRealm;
    String cacheName;
    String keyStoreFileName;
    String keyStorePassword;
    String saslMech;
    String trustStoreFileName;
    String trustStorePassword;
    String userName;
    String userPassword;

    private HotRodServer server = null;

    @Override
    public Map<String, String> start() {
        return Collections.emptyMap();
    }

    private Map<String, String> loadProperties() {
        final Map<String, String> m = new HashMap<>();
        final Properties p = new Properties();

        try {
            p.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            LOGGER.info("Exception occured while reading properties", e);
            throw new RuntimeException(e);
        }

        for (Entry<Object, Object> e : p.entrySet()) {
            final String key = e.getKey().toString();
            final String value = e.getValue().toString();
            HotRodServerProperties.setValue(key, value);
            m.put(key, value);
        }

        authRealm = HotRodServerProperties.AUTH_REALM.getValue();
        cacheName = HotRodServerProperties.DATAGRID_CACHE_NAME.getValue();
        keyStoreFileName = HotRodServerProperties.HOTROD_SERVER_KEYSTORE_FILE.getValue();
        keyStorePassword = HotRodServerProperties.HOTROD_SERVER_KEYSTORE_PASSWD.getValue();
        trustStoreFileName = HotRodServerProperties.HOTROD_SERVER_TRUSTSTORE_FILE.getValue();
        saslMech = HotRodServerProperties.SASL_MECH.getValue();
        trustStorePassword = HotRodServerProperties.HOTROD_SERVER_TRUSTSTORE_PASSWD.getValue();
        userName = HotRodServerProperties.AUTH_USER_NAME.getValue();
        userPassword = HotRodServerProperties.AUTH_USER_PASSWD.getValue();

        return m;
    }

    @Override
    public void init(Map<String, String> initArgs) {
        LOGGER.warnf("::start() - INFINISPAN SERVER IS STARTING...");
        LOGGER.infof("Infinispan server wotking directory: %s", (new java.io.File("")).getAbsolutePath());
        try {
            realInit();
        } catch (Exception e) {
            LOGGER.debugf(e, "Exeption occured", e.getMessage());
        }
    }

    private void realInit() throws Exception {
        final String MAVEN_TARGET_DIR = "target";
        final String INFINSPAN_PERSISTENCE_DIR = "infinispan-persistence";

        final long start = System.currentTimeMillis();
        loadProperties();

        // DefaultCacheManager cm = new DefaultCacheManager("infinispan.xml");
        final GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        gcb.nonClusteredDefault();

        gcb.globalState().enable().persistentLocation(INFINSPAN_PERSISTENCE_DIR, MAVEN_TARGET_DIR)
                .configurationStorage(ConfigurationStorage.CUSTOM)
                .configurationStorageSupplier(() -> new VolatileLocalConfigurationStorage() {
                    @Override
                    public void validateFlags(java.util.EnumSet<CacheContainerAdmin.AdminFlag> flags) {
                        final File infDir = new File(MAVEN_TARGET_DIR + "/" + INFINSPAN_PERSISTENCE_DIR);
                        final String[] files = infDir.list();
                        for (String f : files) {
                            new File(f).delete();
                        }
                    }
                });
        final DefaultCacheManager cm = new DefaultCacheManager(gcb.build());
        cm.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(
                ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, new ConfigurationBuilder().build());

        final HotRodServerConfigurationBuilder hrb = new HotRodServerConfigurationBuilder();

        hrb.port(Integer.valueOf(HotRodServerProperties.HOTROD_SERVER_PORT.getValue()));

        hrb.adminOperationsHandler(new EmbeddedServerAdminOperationHandler());
        hrb.authentication().enable();
        final SimpleServerAuthenticationProvider ssap = new SimpleServerAuthenticationProvider();
        ssap.addUser(userName, authRealm, userPassword.toCharArray());
        hrb.authentication().addAllowedMech(saslMech).serverAuthenticationProvider(ssap).serverName("localhost")
                .securityRealm(authRealm);

        hrb.ssl().enable().keyStoreFileName(keyStoreFileName).keyStorePassword(keyStorePassword.toCharArray())
                .keyAlias("datagrid").keyStoreCertificatePassword(keyStorePassword.toCharArray());
        if ("".equals(HotRodServerProperties.HOTROD_SERVER_KEYSTORE_TYPE.getValue()) == false) {
            hrb.ssl().keyStoreType(HotRodServerProperties.HOTROD_SERVER_KEYSTORE_TYPE.getValue());
        }

        hrb.ssl().trustStoreFileName(trustStoreFileName).trustStorePassword(trustStorePassword.toCharArray());
        if ("".equals(HotRodServerProperties.HOTROD_SERVER_KEYSTORE_TYPE.getValue()) == false) {
            hrb.ssl().trustStoreType(HotRodServerProperties.HOTROD_SERVER_TRUSTSTORE_TYPE.getValue());
        }

        // don't use mutual
        hrb.ssl().requireClientAuth(false);

        // allow any name
        // hrb.ssl().sniHostName("127.0.0.1");
        server = new HotRodServer();
        // don't check configuration build(false)
        server.start(hrb.build(false), cm);
        LOGGER.infof("HOT ROD SERVER: %s, strtup time(ms) %s", server, System.currentTimeMillis() - start);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private static class HotRodServerProperties {

        static Map<String, HotRodServerProperties> MAP = new HashMap<>();

        static final HotRodServerProperties AUTH_REALM = factory("quarkus.infinispan-client.auth-realm", "default");
        static final HotRodServerProperties AUTH_USER_NAME = factory("quarkus.infinispan-client.auth-username",
                "developer");
        static final HotRodServerProperties AUTH_USER_PASSWD = factory("quarkus.infinispan-client.auth-password",
                "developer");
        static final HotRodServerProperties SASL_MECH = factory("quarkus.infinispan-client.sasl-mechanism", "CRAM-MD5");
        static final HotRodServerProperties DATAGRID_CACHE_NAME = factory("datagrid.cache.name", "quarkus");
        static final HotRodServerProperties HOTROD_SERVER_KEYSTORE_FILE = factory("hotrod.server.keystore-file",
                "datagrid.pfx");
        static final HotRodServerProperties HOTROD_SERVER_KEYSTORE_PASSWD = factory("hotrod.server.keystore-password",
                "changeit");
        static final HotRodServerProperties HOTROD_SERVER_PORT = factory("hotrod.server.port", "11223");
        static final HotRodServerProperties HOTROD_SERVER_KEYSTORE_TYPE = factory("hotrod.server.truststore-type", "");
        static final HotRodServerProperties HOTROD_SERVER_TRUSTSTORE_FILE = factory("hotrod.server.truststore-file",
                "trustStore.jks");
        static final HotRodServerProperties HOTROD_SERVER_TRUSTSTORE_PASSWD = factory(
                "hotrod.server.truststore-password", "changeit");
        static final HotRodServerProperties HOTROD_SERVER_TRUSTSTORE_TYPE = factory("hotrod.server.truststore-type",
                "");

        // private final String NAME;
        private final String DEFAULT_VALUE;
        private String value = null;
        private String testValue = null;

        private HotRodServerProperties(@NotNull String defaultValue) {
            // this.NAME = name;
            this.DEFAULT_VALUE = defaultValue;
        }

        private static HotRodServerProperties factory(@NotBlank String name, String defaultValue) {
            final HotRodServerProperties p = new HotRodServerProperties(defaultValue);
            if (MAP == null) {
                MAP = new HashMap<>();
            }
            MAP.put(name, p);
            return p;
        }

        static void setValue(@NotBlank String name, String value) {
            // LOGGER.debugf("Set property(%s) = %s", name, value);
            HotRodServerProperties p;
            if (MAP.containsKey(name)) {
                LOGGER.debugf("Set property(%s) = %s", name, value);
                p = MAP.get(name);
                p.setValue(value);
                return;
            }
            final String TEST_PREFIX = "%test.";
            if (name.startsWith(TEST_PREFIX)) {
                final String testKey = name.substring(TEST_PREFIX.length());
                if (MAP.containsKey(testKey)) {
                    LOGGER.infof("Set TEST PROFILE property: %s = %s", testKey, value);
                    p = MAP.get(testKey);
                    p.setTestValue(value);
                    return;
                } else {
                    LOGGER.debugf("TEST PROFILE property ignored for %s: %s = %s",
                            TestInfinispanServer.class.getSimpleName(), testKey, value);
                }
            }
        }

        private void setValue(String value) {
            this.value = value;
        }

        private void setTestValue(String value) {
            this.testValue = value;
        }

        String getValue() {
            if (testValue != null) {
                return testValue;
            }
            if (value != null) {
                return value;
            }
            return DEFAULT_VALUE;
        }
    }
}
