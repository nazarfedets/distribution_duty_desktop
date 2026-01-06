package com.example.distribution_duty_desktop.service;

import com.example.distribution_duty_desktop.entity.User;
import com.example.distribution_duty_desktop.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public void save(User user) {
        userRepository.save(user);
    }
}
