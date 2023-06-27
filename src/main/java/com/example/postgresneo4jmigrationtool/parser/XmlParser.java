package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.jcabi.xml.XML;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class XmlParser implements Parser {

    private final NodeParser nodeParser;
    private final RelationshipParser relationshipParser;
    private final InnerFieldParser innerFieldParser;

    private final XML xml;

    @Override
    public void parse() {
        XML root = xml.nodes("migration").get(0);
        try {
            List<XML> nodeMigration = root.nodes("node");
            List<XML> relationshipMigration = root.nodes("relationship");
            List<XML> fieldMigration = root.nodes("innerField");
            if (!nodeMigration.isEmpty()) {
                List<XML> tables = getXMLTables(nodeMigration.get(0));
                nodeParser.setTables(tables);
                nodeParser.parse();
            }
            if (!relationshipMigration.isEmpty()) {
                List<XML> tables = getXMLTables(relationshipMigration.get(0));
                relationshipParser.setTables(tables);
                relationshipParser.parse();
            }
            if (!fieldMigration.isEmpty()) {
                List<XML> tables = getXMLTables(fieldMigration.get(0));
                innerFieldParser.setTables(tables);
                innerFieldParser.parse();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("Unsupported migration type. Supported are: " + Arrays.stream(MigrationType.values()).map(s -> s.name().toLowerCase()).toList());
        }
    }

    private List<XML> getXMLTables(XML root) {
        List<XML> tablesTag = root.nodes("tables");
        if (tablesTag.isEmpty()) {
            throw new InvalidConfigurationException("You must provide <tables> tag.");
        }
        List<XML> tables = tablesTag.get(0)
                .nodes("table");
        if (tables.isEmpty()) {
            throw new InvalidConfigurationException("You must provide at least one <table> tag.");
        }
        return tables;
    }

}
