package com.klinec.deserialize.server;

import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.server.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;

@Configuration
public class ContainerConfiguration {
    private final static Logger LOG = LoggerFactory.getLogger(ContainerConfiguration.class);

    @Bean
    @DependsOn(value = "yaml-config")
    public ServletWebServerFactory servletContainer(
            @Value("${keystore.file}") String keystoreFile,
            @Value("${server.port}") final String serverPort,
            @Value("${server.https}") final boolean httpsEnabled,
            @Value("${keystore.pass}") final String keystorePass) throws Exception {

        // This is boilerplate code to set up HTTPS on embedded Tomcat
        // with Spring Boot 2.x:

        final String absoluteKeystoreFile = new File(keystoreFile).getAbsolutePath();

        TomcatServletWebServerFactory tomcatFactory = new TomcatServletWebServerFactory();

        // Set the server port
        tomcatFactory.setPort(Integer.parseInt(serverPort));

        if (httpsEnabled) {
            // Enable SSL
            tomcatFactory.setSsl(true);
            tomcatFactory.setSslKeyStore(absoluteKeystoreFile);
            tomcatFactory.setSslKeyStorePassword(keystorePass);
            tomcatFactory.setSslKeyStoreType("JKS");
            tomcatFactory.setSslKeyAlias("tomcat");

            LOG.info("HTTPS enabled");

        } else {
            // Disable SSL
            tomcatFactory.setSsl(false);
        }

        return tomcatFactory;
    }
}
