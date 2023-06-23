package com.example.postgresneo4jmigrationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {

    private String[] names;
    private Object[] values;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{");
        for (int i = 0; i < names.length; i++) {
            result.append(names[i]).append(": ");
            if (values[i] instanceof String) {
                result.append("\"");
                result.append(values[i]);
                result.append("\"");
            } else {
                result.append(values[i]);
            }
            result.append(", ");
        }
        result.delete(result.lastIndexOf(", "), result.length() - 1);
        result.append("}");
        return result.toString();
    }

}
