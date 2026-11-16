package com.labresource.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class PlatformTimeConfig {

    @Value("${app.time-zone:Asia/Kolkata}")
    private String timeZone;

    @Bean
    public ZoneId platformTimeZone() {
        return ZoneId.of(timeZone);
    }
}