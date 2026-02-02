package com.lucasmoraist.s3_backup.config;

import com.lucasmoraist.s3_backup.model.User;
import com.lucasmoraist.s3_backup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteService implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        User user1 = new User(null, "John Doe", "johndoe@example.com");
        User user2 = new User(null, "Jane Smith", "janesmith@example.com");

        userRepository.findByEmail(user1.getEmail())
                .orElseGet(() -> userRepository.save(user1));
        userRepository.findByEmail(user2.getEmail())
                .orElseGet(() -> userRepository.save(user2));
    }

}
