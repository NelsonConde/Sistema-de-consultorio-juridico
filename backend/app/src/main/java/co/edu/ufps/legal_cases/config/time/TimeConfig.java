package co.edu.ufps.legal_cases.config.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    @Bean
    Clock applicationClock() {
        return Clock.systemDefaultZone();
    }
}