package com.klinec.deserialize.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Override properties file location / configuration file.
 *
 * http://stackoverflow.com/questions/25855795/spring-boot-and-multiple-external-configuration-files
 * Created by dusanklinec on 01.09.16.
 */
@Configuration
class PropertiesConfiguration {
    private final static Logger LOG = LoggerFactory.getLogger(PropertiesConfiguration.class);
    private final static String[] PROPERTIES_FILENAMES = {"default.properties"};
    private static final String CONFIG_FILE_NAME = "appConfig.yml";

    @Value("${properties.location}")
    private String propertiesLocation;

    @Value("${config.location}")
    private String configLocation;

    @Deprecated
    public PropertySourcesPlaceholderConfigurer configProperties() {
        final PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
        for(String file : PROPERTIES_FILENAMES){
            final Properties props = loadProperties(file);
            if (props == null){
                continue;
            }

            propConfig.setProperties(props);
        }

        propConfig.setOrder(100); // high value = low priority
        return propConfig;
    }

    @Bean(name = "yaml-config")
    public PropertySourcesPlaceholderConfigurer configYaml() {
        final PropertySourcesPlaceholderConfigurer propConfig = new PropertySourcesPlaceholderConfigurer();
        final List<Properties> propList = new LinkedList<>();

        // Load properties at first
        for (String file : PROPERTIES_FILENAMES) {
            final Properties props = loadProperties(file);
            if (props == null) {
                continue;
            }

            propList.add(props);
        }

        // Locate YAML
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        final String candidateFile = getCustomConfigPath();
        LOG.info("Candidate YML config {}", candidateFile);

        final Resource[] possiblePropertiesResources = {
                new PathResource(candidateFile),
                new PathResource("config/" + CONFIG_FILE_NAME),
                new PathResource(CONFIG_FILE_NAME),
                new ClassPathResource(CONFIG_FILE_NAME)
        };

        Resource resource = null;
        // Check each resource manually
        for (Resource res : possiblePropertiesResources) {
            if (res.exists()) {
                resource = res;
                break; // Once we find an existing resource, stop
            }
        }

        if (resource == null) {
            return propConfig; // No resource found, return empty config
        }

        // Log which file was actually used.
        try {
            LOG.info("Using config file: {}", resource.getFile());
        } catch (Exception e) {
            LOG.error("Could not get file info", e);
        }

        yaml.setResources(resource);
        propList.add(yaml.getObject());

        propConfig.setPropertiesArray(propList.toArray(new Properties[propList.size()]));
        propConfig.setOrder(-100); // high value = low priority
        return propConfig;
    }

    private Properties loadProperties(final String filename) {
        final Resource[] possiblePropertiesResources = {
                new ClassPathResource(filename),
                new PathResource("config/" + filename),
                new PathResource(filename),
                new PathResource(getCustomPath(filename))
        };

        Resource resource = null;
        // Check each resource manually
        for (Resource res : possiblePropertiesResources) {
            if (res.exists()) {
                resource = res;
                break; // Once we find an existing resource, stop
            }
        }

        if (resource == null) {
            return null; // No valid resource found
        }

        final Properties properties = new Properties();
        try {
            properties.load(resource.getInputStream());
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        LOG.info("Using {} as user resource", resource);

        return properties;
    }

    private String getCustomPath(final String filename) {
        if (propertiesLocation == null) {
            return filename;
        }
        return propertiesLocation.endsWith(".properties") ? propertiesLocation : propertiesLocation + filename;
    }

    private String getCustomConfigPath() {
        final String systemLoc = System.getProperty("config.location");
        if (systemLoc != null) {
            return getCustomWithPath(systemLoc);
        }

        return getCustomWithPath(configLocation);
    }

    private String getCustomWithPath(String path) {
        if (path == null) {
            return CONFIG_FILE_NAME;
        }
        return path.endsWith(".yml") ? path : path + "/" + CONFIG_FILE_NAME;
    }

}
