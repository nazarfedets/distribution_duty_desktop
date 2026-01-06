package com.example.distribution_duty_desktop.service;

import com.example.distribution_duty_desktop.entity.User;
import com.example.distribution_duty_desktop.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String passwordFromDb = user.getPassword().trim();

        String cleanRole = user.getRole().toUpperCase().replace("ROLE_", "").trim();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(passwordFromDb)
                .roles(cleanRole)
                .build();
    }
}
