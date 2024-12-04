package com.klinec.deserialize.server;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;  

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;  

import java.security.KeyStore;

@Configuration
public class ContainerConfiguration {

    private final static Logger LOG = LoggerFactory.getLogger(ContainerConfiguration.class);

    @Bean
    @DependsOn(value = "yaml-config")
    public ServletWebServerFactory servletContainer(@Value("${keystore.file}") String keystoreFile,
                                                     @Value("${server.port}") final String serverPort,
                                                     @Value("${server.https}") final boolean httpsEnabled,
                                                     @Value("${keystore.pass}") final String keystorePass) throws Exception {

        Tomcat tomcat = new Tomcat();

        // Set the server port
        tomcat.setPort(Integer.parseInt(serverPort));

        if (httpsEnabled) {
            // Enable SSL
            tomcat.setSecure(true);

            // Configure SSL context using KeyStore
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new File(keystoreFile).toURI().toURL().openStream(), keystorePass.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore,  
 keystorePass.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());  

            trustManagerFactory.init(null); // You might need to set truststore here for client certificate validation

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),  
 null);

            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(Integer.parseInt(serverPort));
            connector.setSecure(true);
            connector.setScheme("https");
            connector.setProperty("SSLEnabled", "true");
            connector.setProperty("sslProtocol", "TLS");
            connector.setProperty("sslContext",  
 sslContext);

            tomcat.getService().addConnector(connector);

            LOG.info("HTTPS enabled");
        } else {
            // Disable SSL (default connector already exists)
            LOG.info("HTTP enabled");
        }

        StandardServer server = (StandardServer) tomcat.getServer();
        server.addIgnoredProtocols("ajp,http2"); // Remove unnecessary protocols (optional)

        return tomcat;
    }
}
