package com.example.postgresneo4jmigrationtool.config;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Scanner;

@Configuration
public class ApplicationConfig {

    @Value("${xml.script.path}")
    private String xmlPath;

    @Bean
    public Scanner scanner() {
        return new Scanner(System.in).useDelimiter("\n");
    }

    @SneakyThrows
    @Bean
    public XML xml() {
        return new XMLDocument(new File(xmlPath));
    }

}
