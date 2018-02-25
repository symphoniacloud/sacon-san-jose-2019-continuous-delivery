package io.symphonia;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SharedTest {

    @Test
    public void testCapitalize() {
        assertEquals("John", Shared.capitalize("john"));
    }
}
