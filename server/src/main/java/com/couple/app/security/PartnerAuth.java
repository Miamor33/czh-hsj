package com.couple.app.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
public class PartnerAuth extends AbstractAuthenticationToken {
    private final Long partnerId;
    private final String partnerKey;
    private final String displayName;

    public PartnerAuth(Long partnerId, String partnerKey, String displayName) {
        super(List.of(new SimpleGrantedAuthority("ROLE_PARTNER")));
        this.partnerId = partnerId;
        this.partnerKey = partnerKey;
        this.displayName = displayName;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return partnerKey;
    }
}
