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
    private String[] labels;

    public Node(String name, String value, String type, String label) {
        this.names = new String[]{name};
        this.values = new String[]{value};
        this.types = new String[]{type};
        this.labels = new String[]{label};
    }

    public Node(String[] names, Object[] values, String[] types, String[] labels) {
        this.names = names;
        this.types = types;
        this.values = values;
        if (values.length < types.length) {
            this.values = Arrays.copyOf(values, values.length + 1);
        }
        this.labels = labels;
    }

    public String getLabel() {
        return labels.length > 0 ? labels[0] : "";
    }

    public void setLabel(String label) {
        if (labels.length > 0) {
            labels[0] = label;
        } else {
            labels = new String[]{label};
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
                case "integer", "bigint", "bigserial" -> {
                    if (((String) values[i]).isEmpty()) {
                        result.append("null");
                    } else {
                        result.append(values[i]);
                    }
                }
                case "boolean", "bool" -> result.append(values[i].equals("t"));
                case "timestamp", "timestamp without time zone" -> {
                    try {
                        if (timeFormat == null || timeFormat.isEmpty()) {
                            String resultTime = Timestamp.valueOf((String) values[i]).toString();
                            result.append("\"");
                            result.append(resultTime);
                            result.append("\"");
                        } else {
                            Timestamp time = Timestamp.valueOf(values[i].toString());
                            LocalDateTime localDateTime = time.toLocalDateTime();
                            OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                            String resultTime = offsetDateTime.format(formatter);
                            result.append("\"");
                            result.append(resultTime);
                            result.append("\"");
                        }
                    } catch (DateTimeException e) {
                        throw new InvalidConfigurationException("Invalid time format configuration: " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        result.append("null");
                    }
                }
                default -> {
                    if (values[i].equals("\"\"")) {
                        result.append("\"\"");
                    } else if (((String) values[i]).isEmpty()) {
                        result.append("null");
                    } else if (((String) values[i]).startsWith("\"") && ((String) values[i]).endsWith("\"")) {
                        if (((String) values[i]).endsWith("\"\"\"")) {
                            values[i] = ((String) values[i]).substring(((String) values[i]).length() - 3) + "\"";
                        }
                        String s = ((String) values[i]).replaceAll("\"\"\"", "\"\\\\\"")
                                .replaceAll("\"\"", "\\\\\"");
                        result.append(s);
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
