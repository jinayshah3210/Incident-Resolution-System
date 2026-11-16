
package com.irs.backend.config;

import com.irs.backend.model.Admin;
import com.irs.backend.model.Employee;
import com.irs.backend.model.Technician;
import com.irs.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(
            name = "app.seed.enabled",
            havingValue = "true"
    )
    public CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.password}") String seedPassword) {

        return args -> {

            if (!userRepository.existsByEmailIgnoreCase(
                    "employee@itsm.local")) {

                Employee employee = new Employee(
                        "Test Employee",
                        "employee@itsm.local",
                        passwordEncoder.encode(seedPassword),
                        "IT"
                );

                userRepository.save(employee);
            }

            if (!userRepository.existsByEmailIgnoreCase(
                    "admin@itsm.local")) {

                Admin admin = new Admin(
                        "Test Admin",
                        "admin@itsm.local",
                        passwordEncoder.encode(seedPassword),
                        "IT"
                );

                userRepository.save(admin);
            }

            if (!userRepository.existsByEmailIgnoreCase(
                    "technician@itsm.local")) {

                Technician technician = new Technician(
                        "Test Technician",
                        "technician@itsm.local",
                        passwordEncoder.encode(seedPassword),
                        "IT"
                );

                technician.setSkills(
                        List.of("Network", "Hardware", "Software")
                );

                userRepository.save(technician);
            }

            System.out.println("Development users initialized.");
        };
    }
}
