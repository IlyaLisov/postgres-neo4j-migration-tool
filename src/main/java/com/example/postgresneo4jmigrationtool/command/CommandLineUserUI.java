package com.example.postgresneo4jmigrationtool.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class CommandLineUserUI implements UserUI {

    private final Scanner scanner;

    @Override
    public void run() {
        System.out.println("It is running!");
        System.out.println("Would you like to continue? (y/n)");
        String answer = read();
        switch (answer) {
            case "y" -> write("ok, after some changes :)");
            case "n" -> write("Bye!");
            default -> write("Incorrect.");
        }
    }

    @Override
    public String read() {
        return scanner.next();
    }

    @Override
    public void write(String message) {
        System.out.print(message);
    }

}
