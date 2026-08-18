package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.Anniversary;
import com.couple.app.mapper.AnniversaryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnniversaryService {
    private final AnniversaryMapper anniversaryMapper;

    public AnniversaryService(AnniversaryMapper anniversaryMapper) {
        this.anniversaryMapper = anniversaryMapper;
    }

    public List<Anniversary> list() {
        return anniversaryMapper.selectList(new LambdaQueryWrapper<Anniversary>()
                .orderByAsc(Anniversary::getEventDate));
    }

    public Anniversary create(String title, LocalDate eventDate, boolean yearly, Long partnerId) {
        Anniversary a = new Anniversary();
        a.setTitle(title);
        a.setEventDate(eventDate);
        a.setYearly(yearly);
        a.setCreatedBy(partnerId);
        a.setCreatedAt(LocalDateTime.now());
        anniversaryMapper.insert(a);
        return a;
    }

    public Anniversary update(Long id, String title, LocalDate eventDate, boolean yearly) {
        Anniversary a = anniversaryMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("纪念日不存在");
        }
        a.setTitle(title);
        a.setEventDate(eventDate);
        a.setYearly(yearly);
        anniversaryMapper.updateById(a);
        return a;
    }

    public void delete(Long id) {
        anniversaryMapper.deleteById(id);
    }

    public Map<String, Object> nextAnniversary() {
        LocalDate today = LocalDate.now();
        return anniversaryMapper.selectList(null).stream()
                .map(a -> {
                    LocalDate next = nextOccurrence(a, today);
                    long daysLeft = ChronoUnit.DAYS.between(today, next);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("eventDate", a.getEventDate().toString());
                    m.put("nextDate", next.toString());
                    m.put("daysLeft", daysLeft);
                    m.put("yearly", a.getYearly());
                    return m;
                })
                // 「下一个」只看今天及未来；已过期且不重复的不参与
                .filter(m -> (Long) m.get("daysLeft") >= 0)
                .min(Comparator.comparingLong(m -> (Long) m.get("daysLeft")))
                .orElse(null);
    }

    public List<Map<String, Object>> upcoming() {
        LocalDate today = LocalDate.now();
        return anniversaryMapper.selectList(null).stream()
                .map(a -> {
                    LocalDate next = nextOccurrence(a, today);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("eventDate", a.getEventDate().toString());
                    m.put("nextDate", next.toString());
                    m.put("daysLeft", ChronoUnit.DAYS.between(today, next));
                    m.put("yearly", a.getYearly());
                    return m;
                })
                // 未来在前；已过期（不重复）按「距今更近」排在后面
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> (Long) m.get("daysLeft") < 0)
                        .thenComparingLong(m -> {
                            long d = (Long) m.get("daysLeft");
                            return d >= 0 ? d : -d;
                        }))
                .toList();
    }

    private LocalDate nextOccurrence(Anniversary a, LocalDate today) {
        if (!Boolean.TRUE.equals(a.getYearly())) {
            return a.getEventDate().isBefore(today) ? a.getEventDate() : a.getEventDate();
        }
        LocalDate candidate = LocalDate.of(today.getYear(), a.getEventDate().getMonth(),
                Math.min(a.getEventDate().getDayOfMonth(), a.getEventDate().getMonth().length(today.isLeapYear())));
        if (candidate.isBefore(today)) {
            candidate = candidate.plusYears(1);
            candidate = LocalDate.of(candidate.getYear(), a.getEventDate().getMonth(),
                    Math.min(a.getEventDate().getDayOfMonth(), a.getEventDate().getMonth().length(candidate.isLeapYear())));
        }
        return candidate;
    }
}
