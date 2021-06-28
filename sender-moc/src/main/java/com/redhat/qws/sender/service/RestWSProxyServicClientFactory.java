package com.redhat.qws.sender.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

@ApplicationScoped
class RestWSProxyServicClientFactory {
    private static final Logger LOGGER = Logger.getLogger(RestWSProxyServicClientFactory.class);

    @ConfigProperty(name = "ws-proxy/mp-rest/url", defaultValue = "http://127.0.0.1:8080")
    URI restWSPSUrl;

    @ConfigProperty(name = "ws-proxy/mp-rest/keyStore", defaultValue = "defaultKeyStore.pfx")
    String restWSPSKeyStore;

    @ConfigProperty(name = "ws-proxy/mp-rest/keyStorePassword", defaultValue = "changeme")
    String restWSPSKeyStorePassword;

    @ConfigProperty(name = "ws-proxy/mp-rest/trustStore", defaultValue = "trustStore.jks")
    String restWSPSTrustStore;

    @ConfigProperty(name = "ws-proxy/mp-rest/trustStorePassword", defaultValue = "changeme")
    String restWSPSTrustStorePassword;

    @ConfigProperty(name = "ws-proxy.service.discovery", defaultValue = "service-name")
    String restWSPSDiscovery;

    private final Map<String, RestWSProxyService> clients = new ConcurrentHashMap<>();
    private KeyStore keyStore = null;
    private KeyStore trustStore = null;

    private boolean serviceSecured;

    @PostConstruct
    void init() {
        serviceSecured = "https".equalsIgnoreCase(restWSPSUrl.getScheme());
        if (serviceSecured == false) {
            return;
        }
        try {
            keyStore = KeyStore.getInstance(new File(restWSPSKeyStore), restWSPSKeyStorePassword.toCharArray());
            trustStore = KeyStore.getInstance(new File(restWSPSTrustStore), restWSPSTrustStorePassword.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            LOGGER.warnf(e, "EXception occured while initialization");
            throw new RuntimeException(e);
        }
    }

    RestWSProxyService getRestWSProxyService(String ip) {
        final String url;
        if ("pod-ip".equalsIgnoreCase(restWSPSDiscovery)) {
            LOGGER.warnf("'%s' property set to '%s', disabling TLS", "ws-proxy.service.discovery", restWSPSDiscovery);
            url = new StringBuffer().append("http://").append(ip).append(':')
                    .append(restWSPSUrl.getPort()).toString();
        } else {
            LOGGER.debugf("Service discovery method is '%s', ignoring ip [%s]", restWSPSDiscovery, ip);
            url = restWSPSUrl.toString();
        }
        if (clients.containsKey(url)) {
            LOGGER.debugf("%s for URL(%s) found in cache", RestWSProxyService.class.getSimpleName(), url);
            return clients.get(url);
        }
        final URI uri = URI.create(url);
        LOGGER.debugf("Creating new instance of %s(%s)", RestWSProxyService.class.getSimpleName(), url);
        RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri(uri);
        if (serviceSecured) {
            builder.keyStore(keyStore, restWSPSKeyStorePassword).trustStore(trustStore);
        }
        return builder.build(RestWSProxyService.class);
    }
}
