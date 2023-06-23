package com.example.postgresneo4jmigrationtool.config;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ApplicationConfig {

    @Value("${xml.script.path}")
    private String xmlPath;

    @SneakyThrows
    @Bean
    public XML xml() {
        return new XMLDocument(new File(xmlPath));
    }

}
