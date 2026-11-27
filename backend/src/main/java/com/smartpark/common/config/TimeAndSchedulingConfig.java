package com.smartpark.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Clock kao bean omogucava da testovi "zamrznu" vreme.
 * EnableScheduling ukljucuje periodicni posao koji obradjuje istekle rezervacije (FR-11).
 */
@Configuration
@EnableScheduling
public class TimeAndSchedulingConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
