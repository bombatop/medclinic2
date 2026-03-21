package com.medclinic.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    /** When true, refresh_token cookie is marked Secure (required for HTTPS production). */
    private boolean refreshCookieSecure = false;
}
