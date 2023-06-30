package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.jcabi.xml.XML;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class TextXpath {

    private final XML xml;

    public String getAttribute(String attribute) {
        Node value = xml.node()
                .getAttributes()
                .getNamedItem(attribute);
        if (value == null) {
            throw new InvalidConfigurationException("Table must contain " + attribute + " attribute.");
        }
        return value.getNodeValue();
    }

    public String getValue() {
        return xml.xpath("text()")
                .get(0);
    }

    public String getInnerValue(String node) {
        List<XML> type = xml.nodes(node);
        return type.get(0)
                .xpath("text()")
                .get(0);
    }

    public String getInnerValue(String node, String tag) {
        List<XML> tags = xml.nodes(node)
                .get(0)
                .nodes(tag);
        if (!tags.isEmpty()) {
            return tags
                    .get(0)
                    .xpath("text()")
                    .get(0);
        } else {
            return "";
        }
    }

    public Map<String, List<String>> getPreferredColumns(String tag) {
        List<XML> followColumns = xml.nodes(tag);
        if (!followColumns.isEmpty()) {
            List<XML> columns = followColumns.get(0)
                    .nodes("column");
            List<String> columnNames = columns.stream()
                    .map(c -> new TextXpath(c).getValue())
                    .toList();
            List<String> values = columns.stream()
                    .map(c -> new TextXpath(c).getAttribute("value"))
                    .toList();
            if (columnNames.size() != values.size()) {
                throw new InvalidConfigurationException("Amount of column names and values must be the same.");
            }
            Map<String, List<String>> followMap = new LinkedHashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                List<String> list;
                if (followMap.containsKey(columnNames.get(i))) {
                    list = followMap.get(columnNames.get(i));
                } else {
                    list = new ArrayList<>();
                }
                list.add(values.get(i));
                followMap.put(columnNames.get(i), list);
            }
            return followMap;
        }
        return new LinkedHashMap<>();
    }

}
