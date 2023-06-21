package com.example.postgresneo4jmigrationtool.command;

public interface UserUI {

    void run();

    String read();

    void write(String message);

}
