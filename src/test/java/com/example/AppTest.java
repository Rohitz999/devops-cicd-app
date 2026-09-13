package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Test
    void testHealth() {
        App app = new App();
        assertEquals("OK", app.health());
    }

    @Test
    void testVersion() {
        App app = new App();
        assertEquals("v1.0.0", app.version());
    }

    @Test
    void testApi() {
        App app = new App();
        assertEquals("DevOps Mega Project - CI/CD Pipeline Working!", app.api());
    }
}
