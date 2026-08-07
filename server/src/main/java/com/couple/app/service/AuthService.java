package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.Partner;
import com.couple.app.mapper.PartnerMapper;
import com.couple.app.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final PartnerMapper partnerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public AuthService(PartnerMapper partnerMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.partnerMapper = partnerMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> login(String partnerKey, String password) {
        checkRateLimit(partnerKey);
        Partner partner = partnerMapper.selectOne(new LambdaQueryWrapper<Partner>()
                .eq(Partner::getPartnerKey, partnerKey));
        if (partner == null || !passwordEncoder.matches(password, partner.getPasswordHash())) {
            recordFail(partnerKey);
            throw new BusinessException("身份或密码错误");
        }
        attempts.remove(partnerKey);
        String token = jwtService.createToken(partner.getId(), partner.getPartnerKey(), partner.getDisplayName());
        return Map.of(
                "token", token,
                "partnerId", partner.getId(),
                "partnerKey", partner.getPartnerKey(),
                "displayName", partner.getDisplayName()
        );
    }

    private void checkRateLimit(String key) {
        Attempt a = attempts.get(key);
        if (a != null && a.count >= 8 && System.currentTimeMillis() - a.firstAt < 15 * 60_000L) {
            throw new BusinessException("尝试过多，请稍后再试");
        }
        if (a != null && System.currentTimeMillis() - a.firstAt >= 15 * 60_000L) {
            attempts.remove(key);
        }
    }

    private void recordFail(String key) {
        attempts.compute(key, (k, old) -> {
            long now = System.currentTimeMillis();
            if (old == null || now - old.firstAt >= 15 * 60_000L) {
                return new Attempt(1, now);
            }
            return new Attempt(old.count + 1, old.firstAt);
        });
    }

    private record Attempt(int count, long firstAt) {
    }
}
