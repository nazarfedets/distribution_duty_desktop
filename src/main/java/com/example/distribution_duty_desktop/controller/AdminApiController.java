package com.example.distribution_duty_desktop.controller;

import com.example.distribution_duty_desktop.entity.Duty;
import com.example.distribution_duty_desktop.entity.User;
import com.example.distribution_duty_desktop.repository.DutyRepository;
import com.example.distribution_duty_desktop.service.DutyService;
import com.example.distribution_duty_desktop.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // Важливо

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final DutyService dutyService;
    private final UserService userService;
    private final DutyRepository dutyRepository;

    public AdminApiController(DutyService dutyService, UserService userService, DutyRepository dutyRepository) {
        this.dutyService = dutyService;
        this.userService = userService;
        this.dutyRepository = dutyRepository;
    }

    // ... ваші методи getGroupData та updateCell (вони правильні) ...

    // Додайте цей метод (його не було в коді)
    @PostMapping("/update-cell")
    @Transactional
    public ResponseEntity<String> updateCell(@RequestBody Map<String, Object> payload) {
        String login = (String) payload.get("login");
        int day = (int) payload.get("day");
        int year = (int) payload.get("year");
        int month = (int) payload.get("month");
        String status = (String) payload.get("status");

        dutyRepository.findByYearAndMonth(year, month).stream()
                .filter(d -> d.getUser().getLogin().equals(login) && d.getDay() == day)
                .forEach(dutyRepository::delete);

        // Якщо статус не порожній - створюємо новий запис
        if (status != null && !status.isEmpty()) {
            User user = userService.getUserByLogin(login).orElseThrow();
            Duty duty = new Duty();
            duty.setUser(user);
            duty.setDay(day);
            duty.setMonth(month);
            duty.setYear(year);
            duty.setPlace(status.toUpperCase());
            dutyRepository.save(duty);
        }

        return ResponseEntity.ok("Клітинку оновлено");
    }

    @PostMapping("/auto-distribute")
    @Transactional
    public ResponseEntity<String> autoDistribute(@RequestBody Map<String, Object> payload) {
        String groupName = (String) payload.get("groupName");
        int year = (int) payload.get("year");
        int month = (int) payload.get("month");

        // ОТРИМУЄМО ГЛОБАЛЬНИЙ ЛІМІТ З ПАКЕТУ
        int globalLimit = payload.containsKey("globalLimit") ? (int) payload.get("globalLimit") : 4;

        List<User> students = userService.findAll().stream()
                .filter(u -> groupName.equals(u.getGroupName()))
                .collect(Collectors.toList());

        // Видаляємо лише автоматичні наряди (П), залишаючи штрафи (Ш) та звільнення (З)
        dutyRepository.deleteByYearAndMonthAndPlace(year, month, "П");

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            if (isDayOccupied(day, year, month)) continue;

            // ПЕРЕДАЄМО GLOBAL LIMIT В МЕТОД ПОШУКУ
            User candidate = findBestCandidate(students, day, year, month, globalLimit);

            if (candidate != null) {
                Duty autoDuty = new Duty();
                autoDuty.setUser(candidate);
                autoDuty.setYear(year);
                autoDuty.setMonth(month);
                autoDuty.setDay(day);
                autoDuty.setPlace("П");
                dutyRepository.save(autoDuty);
            }
        }

        return ResponseEntity.ok("Розподіл завершено");
    }

    private User findBestCandidate(List<User> students, int day, int year, int month, int globalLimit) {
        List<Duty> allMonthDuties = dutyRepository.findByYearAndMonth(year, month);

        // Створюємо копію списку і перемішуємо, щоб розподіл був "живим"
        List<User> shuffledStudents = new ArrayList<>(students);
        Collections.shuffle(shuffledStudents);

        return shuffledStudents.stream()
                .filter(s -> {
                    // 1. Немає звільнення (З) або іншого наряду в цей день
                    return allMonthDuties.stream()
                            .noneMatch(d -> d.getUser().getLogin().equals(s.getLogin()) && d.getDay() == day);
                })
                .filter(s -> {
                    // 2. Інтервал 3 дні
                    return allMonthDuties.stream()
                            .filter(d -> d.getUser().getLogin().equals(s.getLogin()) &&
                                    ("П".equals(d.getPlace()) || "Ш".equals(d.getPlace())))
                            .noneMatch(d -> Math.abs(d.getDay() - day) < 3);
                })
                .filter(s -> {
                    // 3. Перевірка лімітів (ПЕРСОНАЛЬНИЙ АБО ГЛОБАЛЬНИЙ)
                    long currentCount = allMonthDuties.stream()
                            .filter(d -> d.getUser().getLogin().equals(s.getLogin()) &&
                                    ("П".equals(d.getPlace()) || "Ш".equals(d.getPlace())))
                            .count();

                    Integer pLimit = s.getDutyLimit();
                    // Якщо встановлено персональний ліміт (не -1 і не null), беремо його, інакше - глобальний
                    int effectiveLimit = (pLimit != null && pLimit != -1) ? pLimit : globalLimit;

                    return currentCount < effectiveLimit;
                })
                .min(Comparator.comparingLong(s -> countUserDutiesInList(s, allMonthDuties)))
                .orElse(null);
    }

    private boolean isDayOccupied(int day, int year, int month) {
        return dutyRepository.findByYearAndMonth(year, month).stream()
                .anyMatch(d -> d.getDay() == day && ("П".equals(d.getPlace()) || "Ш".equals(d.getPlace())));
    }


    @PostMapping("/update-limit")
    public ResponseEntity<String> updateLimit(@RequestBody Map<String, Object> payload) {
        String login = (String) payload.get("login");
        Object limitObj = payload.get("limit");
        Integer limit = (limitObj != null) ? Integer.parseInt(limitObj.toString()) : null;

        User user = userService.getUserByLogin(login).orElseThrow();
        user.setDutyLimit(limit);
        userService.save(user);

        return ResponseEntity.ok("Ліміт оновлено");
    }

    private long countUserDutiesInList(User u, List<Duty> duties) {
        return duties.stream()
                .filter(d -> d.getUser().getLogin().equals(u.getLogin()) &&
                        ("П".equals(d.getPlace()) || "Ш".equals(d.getPlace())))
                .count();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.findAll().size());
        stats.put("totalDuties", dutyRepository.count());

        // Фільтруємо за літерою "Ш", яку ми реально використовуємо
        long extraDuties = dutyRepository.findAll().stream()
                .filter(d -> "Ш".equals(d.getPlace()))
                .count();
        stats.put("extraDuties", extraDuties);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getAllGroups() {
        List<String> groups = userService.findAll().stream()
                .map(User::getGroupName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/group-data")
    public ResponseEntity<List<Map<String, Object>>> getGroupData(
            @RequestParam String groupName,
            @RequestParam int year,
            @RequestParam int month) {

        // 1. Отримуємо всіх курсантів групи
        List<User> students = userService.findAll().stream()
                .filter(u -> groupName.equals(u.getGroupName()))
                .sorted(Comparator.comparing(User::getPib)) // Сортування за алфавітом
                .toList();

        // 2. Отримуємо всі наряди на цей місяць
        List<Duty> duties = dutyRepository.findByYearAndMonth(year, month);

        List<Map<String, Object>> result = new ArrayList<>();

        for (User student : students) {
            Map<String, Object> row = new HashMap<>();
            row.put("pib", student.getPib()); // Ключове поле для JS
            row.put("login", student.getLogin());
            row.put("limit", student.getDutyLimit());

            // Формуємо карту днів: день -> статус ("П", "Ш", "З")
            Map<Integer, String> daysMap = new HashMap<>();
            duties.stream()
                    .filter(d -> d.getUser().getLogin().equals(student.getLogin()))
                    .forEach(d -> daysMap.put(d.getDay(), d.getPlace()));

            row.put("days", daysMap);
            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/clear-month")
    @Transactional
    public ResponseEntity<String> clearMonth(@RequestBody Map<String, Object> payload) {
        int year = (int) payload.get("year");
        int month = (int) payload.get("month");
        String groupName = (String) payload.get("groupName");

        // Знайти всіх користувачів групи та видалити їхні наряди за цей місяць
        List<User> groupUsers = userService.findAll().stream()
                .filter(u -> groupName.equals(u.getGroupName()))
                .toList();

        for (User user : groupUsers) {
            dutyRepository.findByYearAndMonth(year, month).stream()
                    .filter(d -> d.getUser().getLogin().equals(user.getLogin()))
                    .forEach(dutyRepository::delete);
        }

        return ResponseEntity.ok("Місяць очищено");
    }
}