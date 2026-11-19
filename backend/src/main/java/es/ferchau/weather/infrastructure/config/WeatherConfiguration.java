package es.ferchau.weather.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class WeatherConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("municipalities", "forecasts"));
        cacheManager.setAllowNullValues(false);
        cacheManager.registerCustomCache("municipalities", Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofHours(12))
                .build());
        cacheManager.registerCustomCache("forecasts", Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(60))
                .build());
        return cacheManager;
    }
}
