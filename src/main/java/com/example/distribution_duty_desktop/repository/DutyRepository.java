package com.example.distribution_duty_desktop.repository;

import com.example.distribution_duty_desktop.entity.Duty;
import com.example.distribution_duty_desktop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DutyRepository extends JpaRepository<Duty, Long> {

    List<Duty> findByYearAndMonth(int year, int month);

    List<Duty> findByUser(User user);


    @Transactional
    void deleteByYearAndMonthAndPlace(int year, int month, String place);
}