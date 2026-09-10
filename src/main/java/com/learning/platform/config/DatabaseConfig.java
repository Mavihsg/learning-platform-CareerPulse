package com.learning.platform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL_POOLED:#{null}}")
    private String databaseUrlPooled;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${spring.datasource.url:#{null}}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.username:sa}")
    private String springUsername;

    @Value("${spring.datasource.password:}")
    private String springPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String raw = (databaseUrlPooled != null && !databaseUrlPooled.isBlank())
                ? databaseUrlPooled.trim()
                : ((databaseUrl != null && !databaseUrl.isBlank())
                ? databaseUrl.trim()
                : (springDatasourceUrl != null && !springDatasourceUrl.isBlank() ? springDatasourceUrl.trim() : null));

        // Fallback to local H2 in-memory database
        if (raw == null || raw.contains("jdbc:h2:mem")) {
            log.info("Active Database: Local In-Memory H2 (learningdb).");
            HikariConfig h2Config = new HikariConfig();
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setJdbcUrl("jdbc:h2:mem:learningdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            h2Config.setUsername("sa");
            h2Config.setPassword("");
            return new HikariDataSource(h2Config);
        }

        // Clean quotes if copied from .env file (e.g. DATABASE_URL="postgresql://...")
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw = raw.substring(1, raw.length() - 1);
        }

        log.info("Active Database: Cloud PostgreSQL (Neon.tech). Connecting...");
        HikariConfig pgConfig = new HikariConfig();
        pgConfig.setDriverClassName("org.postgresql.Driver");

        // Parse postgresql://user:pass@host/db URI format commonly provided by Neon and Railway
        if (raw.startsWith("postgresql://") || raw.startsWith("postgres://")) {
            try {
                URI uri = new URI(raw);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // e.g. /neondb
                String query = uri.getQuery(); // e.g. sslmode=require&channel_binding=require

                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    pgConfig.setUsername(parts[0]);
                    pgConfig.setPassword(parts[1]);
                }

                // If Neon host is non-pooled, automatically upgrade to PgBouncer pooler
                if (host != null && host.contains("neon.tech") && !host.contains("-pooler")) {
                    int dotIdx = host.indexOf('.');
                    if (dotIdx > 0) {
                        host = host.substring(0, dotIdx) + "-pooler" + host.substring(dotIdx);
                        log.info("Auto-upgraded Neon host to PgBouncer pooler: {}", host);
                    }
                }

                // Construct clean JDBC URL
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + "?sslmode=require";
                pgConfig.setJdbcUrl(jdbcUrl);
                log.info("Transformed Neon URI into JDBC URL: jdbc:postgresql://{}:{}{}", host, port, path);
            } catch (Exception e) {
                log.warn("Could not parse URI directly, attempting jdbc: prefix: {}", e.getMessage());
                pgConfig.setJdbcUrl(raw.startsWith("jdbc:") ? raw : "jdbc:" + raw);
            }
        } else {
            // Already in JDBC format - upgrade to pooler if direct Neon URL
            if (raw.contains("neon.tech") && !raw.contains("-pooler")) {
                raw = raw.replace(".c-2.us-east-2.aws.neon.tech", "-pooler.c-2.us-east-2.aws.neon.tech");
                log.info("Auto-upgraded raw JDBC URL to Neon PgBouncer pooler endpoint.");
            }
            pgConfig.setJdbcUrl(raw);
            if (springUsername != null) pgConfig.setUsername(springUsername);
            if (springPassword != null) pgConfig.setPassword(springPassword);
        }

        // HikariCP connection pool settings optimized for Neon Serverless
        pgConfig.setMaximumPoolSize(10);
        pgConfig.setMinimumIdle(5);
        pgConfig.setIdleTimeout(300000);
        pgConfig.setMaxLifetime(600000);
        pgConfig.setConnectionTimeout(20000);
        pgConfig.setKeepaliveTime(30000);

        return new HikariDataSource(pgConfig);
    }
}
