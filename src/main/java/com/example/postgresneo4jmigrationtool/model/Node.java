package com.example.postgresneo4jmigrationtool.model;

import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import lombok.Data;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Data
public class Node {

    private String[] names;
    private Object[] values;
    private String[] types;
    private String timeFormat;

    public Node(String name, String value, String type) {
        this.names = new String[]{name};
        this.values = new String[]{value};
        this.types = new String[]{type};
    }

    public Node(String[] names, Object[] values, String[] types) {
        this.names = names;
        this.types = types;
        this.values = values;
        if (values.length < types.length) {
            this.values = Arrays.copyOf(values, values.length + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{");
        for (int i = 0; i < names.length; i++) {
            result.append(names[i]).append(": ");
            if (values[i] == null) {
                break;
            }
            switch (types[i]) {
                case "integer", "bigint", "bigserial" ->
                        result.append(values[i]);
                case "boolean", "bool" -> result.append(values[i].equals("t"));
                case "timestamp", "timestamp without time zone" -> {
                    result.append("\"");
                    if (timeFormat == null || timeFormat.isEmpty()) {
                        result.append(Timestamp.valueOf((String) values[i]));
                    } else {
                        try {
                            Timestamp time = Timestamp.valueOf(values[i].toString());
                            LocalDateTime localDateTime = time.toLocalDateTime();
                            OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                            String resultTime = offsetDateTime.format(formatter);
                            result.append(resultTime);
                        } catch (IllegalArgumentException |
                                 DateTimeException e) {
                            throw new InvalidConfigurationException("Invalid time format configuration: " + e.getMessage());
                        }
                    }
                    result.append("\"");
                }
                default -> {
                    if (values[i].equals("\"\"")) {
                        result.append("\"\"");
                    } else if (((String) values[i]).isEmpty()) {
                        result.append("null");
                    } else {
                        result.append("\"");
                        result.append(values[i]);
                        result.append("\"");
                    }
                }
            }
            result.append(", ");
        }
        result.delete(result.lastIndexOf(", "), result.length() - 1);
        result.append("}");
        return result.toString();
    }

}
