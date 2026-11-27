package com.smartpark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Bez podrazumevanog "user" naloga sa generisanom lozinkom - prijava ide iskljucivo preko JWT-a.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SmartparkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartparkBackendApplication.class, args);
    }
}
