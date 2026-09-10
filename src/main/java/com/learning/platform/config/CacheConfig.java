package com.learning.platform.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // Pre-create all known caches while retaining dynamic creation for any new cache
        for (String name : new String[]{
                "courses", "course", "dashboard", "team_analytics",
                "enrolled_courses", "users", "all_users", "weekly_activity", "leaderboard"
        }) {
            cacheManager.getCache(name);
        }
        return cacheManager;
    }
}
