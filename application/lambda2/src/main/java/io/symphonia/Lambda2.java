package io.symphonia;

public class Lambda2 {
    public String handler(String input) {
        return String.format("Goodbye, %s!", Shared.capitalize(input));
    }
}
