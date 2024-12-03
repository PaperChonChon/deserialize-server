package com.klinec.deserialize.server;

import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;

@Configuration
public class ContainerConfiguration {
    private final static Logger LOG = LoggerFactory.getLogger(ContainerConfiguration.class);

    @Bean
    @DependsOn(value = "yaml-config")
    public EmbeddedServletContainerCustomizer containerCustomizer(
            @Value("${keystore.file}") String keystoreFile,
            @Value("${server.port}") final String serverPort,
            @Value("${server.https}") final boolean httpsEnabled,
            @Value("${keystore.pass}") final String keystorePass)
            throws Exception {

        // This is boiler plate code to setup https on embedded Tomcat
        // with Spring Boot:

        final String absoluteKeystoreFile = new File(keystoreFile)
                .getAbsolutePath();

        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                if (container instanceof TomcatEmbeddedServletContainerFactory) {
                    TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;

                    // Configure the connector directly without using TomcatConnectorCustomizer
                    tomcat.setPort(Integer.parseInt(serverPort));

                    if (httpsEnabled) {
                        tomcat.setSsl(true);
                        tomcat.setSslKeyStore(absoluteKeystoreFile);
                        tomcat.setSslKeyStorePassword(keystorePass);
                        tomcat.setSslKeyStoreType("JKS");
                        tomcat.setSslKeyAlias("tomcat");

                        LOG.info("HTTPS used");
                    } else {
                        tomcat.setSsl(false);
                    }
                }
            }
        };
    }
}
