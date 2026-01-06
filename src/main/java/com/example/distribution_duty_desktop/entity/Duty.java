package com.example.distribution_duty_desktop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "duties")
@Getter @Setter
public class Duty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_login", referencedColumnName = "login")
    private User user;

    private int year;
    private int month;
    private int day;
    private String place;

    public Duty() {}


}