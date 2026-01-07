package com.example.distribution_duty_desktop.service;

import com.example.distribution_duty_desktop.entity.Duty;
import com.example.distribution_duty_desktop.repository.DutyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DutyService {
    private final DutyRepository dutyRepository;

    public DutyService(DutyRepository dutyRepository) {
        this.dutyRepository = dutyRepository;
    }

    public long countAllDuties() {
        return dutyRepository.count();
    }

    public long countExtraDuties() {
        return dutyRepository.findAll().stream()
                .filter(d -> "Позачергово".equals(d.getPlace())) // Фільтруємо ДО підрахунку
                .count();
    }

    public void addDuty(Duty duty) {
        dutyRepository.save(duty);
    }

    public List<Duty> getDutiesMonth(int year, int month) {
        return dutyRepository.findByYearAndMonth(year, month);
    }
}