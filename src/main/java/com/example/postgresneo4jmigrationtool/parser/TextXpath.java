package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.jcabi.xml.XML;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Node;

import java.util.List;

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

    public String getValue(String node) {
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

}
