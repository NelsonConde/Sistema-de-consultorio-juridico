package co.edu.ufps.legal_cases.config.time;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    @Bean
    public ZoneId institutionalTimeZone(
            @Value("${app.time-zone:America/Bogota}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}
