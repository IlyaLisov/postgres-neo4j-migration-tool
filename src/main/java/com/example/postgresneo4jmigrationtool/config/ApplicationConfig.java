package com.example.postgresneo4jmigrationtool.config;

import com.example.postgresneo4jmigrationtool.parser.validator.XmlValidator;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XSD;
import com.jcabi.xml.XSDDocument;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ApplicationConfig {

    @Value("${xml.script.path}")
    private String xmlPath;

    @Value("${xml.validation.schema.path}")
    private String xsdPath;

    @Value("${xml.validation.enabled}")
    private boolean validate;

    @SneakyThrows
    @Bean
    public XML xml() {
        return new XMLDocument(new File(xmlPath));
    }

    @SneakyThrows
    @Bean
    public XSD xsd() {
        return new XSDDocument(new File(xsdPath));
    }

    @Bean
    public XmlValidator xmlValidator(XML xml, XSD xsd) {
        XmlValidator validator = new XmlValidator(xml, xsd);
        if (validate) {
            validator.validate();
        }
        return validator;
    }

}
