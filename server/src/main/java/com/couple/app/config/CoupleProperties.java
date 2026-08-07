package com.couple.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "couple")
public class CoupleProperties {
    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Map<String, PartnerAccount> partners = new LinkedHashMap<>();
    private String togetherDate = "2024-01-01";

    @Data
    public static class Jwt {
        private String secret;
        private long expireHours = 168;
    }

    @Data
    public static class Upload {
        private String dir = "./uploads";
        private int maxFeatured = 12;
    }

    @Data
    public static class PartnerAccount {
        private String displayName;
        private String password;
    }
}
