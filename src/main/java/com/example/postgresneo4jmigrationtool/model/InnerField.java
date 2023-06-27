package com.example.postgresneo4jmigrationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InnerField {

    private Node node;
    private String label;
    private String fieldName;
    private String valueType;
    private List<Object> values;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (Object value : values) {
            switch (valueType) {
                case "integer", "bigint", "bigserial" -> result.append(value);
                default -> {
                    result.append("\"");
                    result.append(value);
                    result.append("\"");
                }
            }
            result.append(", ");
        }
        result.delete(result.lastIndexOf(", "), result.length() - 1);
        result.append("]");
        return result.toString();
    }

}
