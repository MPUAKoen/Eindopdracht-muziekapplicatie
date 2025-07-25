package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only seed if no users exist
        if (userRepository.count() == 0) {
            // 1) Admin
            User admin = new User();
            admin.setName("Site Admin");
            admin.setEmail("admin@email.com");
            admin.setPassword(passwordEncoder.encode("Test123"));
            admin.setInstrument("Piano");
            admin.setRole("ADMIN");
            userRepository.save(admin);

            // 2) Teacher
            User teacher = new User();
            teacher.setName("Jane Teacher");
            teacher.setEmail("teacher@email.com");
            teacher.setPassword(passwordEncoder.encode("Test123"));
            teacher.setInstrument("Piano");
            teacher.setRole("TEACHER");
            userRepository.save(teacher);

            // 3) Regular user/student
            User user = new User();
            user.setName("John Student");
            user.setEmail("user@email.com");
            user.setPassword(passwordEncoder.encode("Test123"));
            user.setInstrument("Piano");
            user.setRole("USER");
            userRepository.save(user);
        }
    }
}
