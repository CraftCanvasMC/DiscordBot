package io.canvasmc.bot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public final class EnvConfig {
    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);

    private final Properties fileProperties;

    private EnvConfig(Properties fileProperties) {
        this.fileProperties = fileProperties;
    }

    public static EnvConfig load() {
        Properties properties = new Properties();
        Path envFile = Path.of(".env");
        if (Files.exists(envFile)) {
            try {
                List<String> lines = Files.readAllLines(envFile);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    int eq = trimmed.indexOf('=');
                    if (eq > 0) {
                        String key = trimmed.substring(0, eq).trim();
                        String value = trimmed.substring(eq + 1).trim();
                        properties.setProperty(key, value);
                    }
                }
                log.info("Loaded .env file");
            } catch (IOException e) {
                log.warn("Failed to read .env file", e);
            }
        }
        return new EnvConfig(properties);
    }

    public String get(String key, String... aliases) {
        Objects.requireNonNull(key, "key");

        String value = getFromFile(key);
        if (isUsable(value)) {
            return value;
        }

        for (String alias : aliases) {
            value = getFromFile(alias);
            if (isUsable(value)) {
                return value;
            }
        }

        value = getFromEnvironment(key);
        if (isUsable(value)) {
            return value;
        }

        for (String alias : aliases) {
            value = getFromEnvironment(alias);
            if (isUsable(value)) {
                return value;
            }
        }

        return null;
    }

    private String getFromFile(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return fileProperties.getProperty(key);
    }

    private String getFromEnvironment(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String direct = System.getenv(key);
        if (isUsable(direct)) {
            return direct;
        }

        String normalized = toEnvStyle(key);
        if (normalized.equals(key)) {
            return null;
        }
        return System.getenv(normalized);
    }

    private static String toEnvStyle(String key) {
        String underscored = key
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('-', '_');
        return underscored.toUpperCase(Locale.ROOT);
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank();
    }
}