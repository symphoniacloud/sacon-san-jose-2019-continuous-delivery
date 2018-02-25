package io.symphonia;

public class Lambda1 {

    public String handler(String input) {
        return String.format("Hello, %s!", Shared.capitalize(input));
    }
}
