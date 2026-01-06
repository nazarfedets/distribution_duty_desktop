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

    public void addDuty(Duty duty) {
        dutyRepository.save(duty);
    }

    public List<Duty> getDutiesMonth(int year, int month, String place) {
        return dutyRepository.findByYearAndMonth(year, month);
    }

    public void deleteDuties(int year, int month, String place) {
        dutyRepository.deleteByYearAndMonthAndPlace(year, month, place);
    }
}
