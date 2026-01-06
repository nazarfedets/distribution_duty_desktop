package com.example.distribution_duty_desktop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    private String login;
    private String password;
    private String pib;
    private String phone;
    private String course;
    private String role;

    @Column(name = "group_name")
    private String groupName;

    public User() {}

    public User(String login, String password, String pib, String phone, String course, String groupName, String role) {
        this.login = login;
        this.password = password;
        this.pib = pib;
        this.phone = phone;
        this.course = course;
        this.groupName = groupName;
        this.role = role;
    }
}
