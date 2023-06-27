package com.example.postgresneo4jmigrationtool;

import com.example.postgresneo4jmigrationtool.parser.Parser;
import com.example.postgresneo4jmigrationtool.parser.XmlParser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@RequiredArgsConstructor
public class PostgresNeo4jMigrationToolApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext appContext = SpringApplication.run(PostgresNeo4jMigrationToolApplication.class, args);
        try {
            Parser parser = appContext.getBean(XmlParser.class);
            parser.parse();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
