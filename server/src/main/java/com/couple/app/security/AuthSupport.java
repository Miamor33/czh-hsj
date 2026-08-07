package com.couple.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthSupport {
    private AuthSupport() {
    }

    public static PartnerAuth requirePartner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof PartnerAuth partnerAuth)) {
            throw new org.springframework.security.access.AccessDeniedException("未登录");
        }
        return partnerAuth;
    }
}
