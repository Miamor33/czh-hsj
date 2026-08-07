package com.couple.app.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class HomeService {
    private final CoverService coverService;
    private final AnniversaryService anniversaryService;
    private final ChallengeService challengeService;
    private final QaService qaService;

    public HomeService(CoverService coverService, AnniversaryService anniversaryService,
                       ChallengeService challengeService, QaService qaService) {
        this.coverService = coverService;
        this.anniversaryService = anniversaryService;
        this.challengeService = challengeService;
        this.qaService = qaService;
    }

    public Map<String, Object> dashboard(Long partnerId) {
        LocalDate together = coverService.getTogetherDate();
        long days = ChronoUnit.DAYS.between(together, LocalDate.now()) + 1;
        Map<String, Object> result = new HashMap<>();
        result.put("loveDays", days);
        result.put("togetherDate", together.toString());
        result.put("upcomingAnniversaries", anniversaryService.upcoming());
        result.put("challengeModules", challengeService.modules());
        result.put("pendingQuestions", qaService.pendingForMe(partnerId));
        return result;
    }
}
