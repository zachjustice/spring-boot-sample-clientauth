package ca.oakey.samples.web.clientauth;

import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@Component
public class SSLContextFactory {
    private static final Logger logger = LoggerFactory.getLogger(SSLContextFactory.class);

    private Map<String, String> certs;

    public SSLContextFactory() throws RuntimeException {
        Properties props = new Properties();
        try {
            props.load(SSLContextFactory.class.getResourceAsStream("/certificates.properties"));
        } catch (IOException e) {
            throw new RuntimeException("loading certificates", e);
        }
        certs = props.entrySet().stream()
                .map(e -> Pair.of((String) e.getKey(), (String) e.getValue()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    public SSLContext createSSLContext() throws Exception {
        /*
         * Sample certs use the same password
         */
        char[] password = "Newclient02".toCharArray();
//        char[] password = "changeme".toCharArray();

        /*
         * Create an SSLContext that uses client.jks as the client certificate
         * and the truststore.jks as the trust material (trusted CA certificates).
         * In this sample, truststore.jks contains ca.pem which was used to sign
         * both client.pfx and server.jks.
         */
        KeyStore keyStore = createKeyStore();
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        InputStream in = getClass().getResourceAsStream("/usom-tax-keystore.jks");
//        keyStore.load(in, password);

        return SSLContextBuilder
                .create()
//                .loadKeyMaterial(loadPfx("/client-nonprod.jks", password), password, null)
                .loadKeyMaterial(loadPfx("/usom-tax-keystore.jks", password), password, null)
                .loadTrustMaterial(keyStore, null)
                .build();
    }

    private KeyStore loadPfx(String file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        InputStream in = getClass().getResourceAsStream(file);
        keyStore.load(in, password);
        return keyStore;
    }

    private TrustManagerFactory createTrustManagerFactory(KeyStore ks) throws NoSuchAlgorithmException,
            KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        return tmf;
    }

    private KeyStore createKeyStore() throws GeneralSecurityException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return appendCertificates(certs.entrySet().stream().map(cert -> {
            try {
                ByteArrayInputStream certBais = new ByteArrayInputStream(cert.getValue().getBytes());
                return Pair.of(cert.getKey(), (X509Certificate) cf.generateCertificate(certBais));
            } catch (CertificateException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
    }

    private KeyStore appendCertificates(Map<String, Certificate> certificates) {
        try {
            X509TrustManager trustManager = getDefaultTrustManager();

            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null);

                if (trustManager != null) {
                    for (X509Certificate cert : trustManager.getAcceptedIssuers()) {
                        trustStore.setCertificateEntry(UUID.randomUUID().toString(), cert);
                        logger.debug("adding existing certificate to truststore {}", cert.getSubjectDN().getName());
                    }
                }

                certificates.forEach((alias, certificate) -> {
                    try {
                        trustStore.setCertificateEntry(alias, certificate);
                        logger.debug("adding new certificate to truststore {}", alias);
                    } catch (SecurityException | KeyStoreException ex) {
                        logger.error("unable to add certificate: {}", certificate);
                        throw new IllegalStateException(ex);
                    }
                });

                return trustStore;
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private X509TrustManager getDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory factory = createTrustManagerFactory((KeyStore) null);
        TrustManager[] trustManagers = factory.getTrustManagers();
        logger.debug("TrustManagers = {}");
        return (X509TrustManager) trustManagers[0];
    }
}
