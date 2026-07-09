package com.chzikon.global.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 레포 루트 .env 를 부팅 시 자동 로드 — IntelliJ ▶ 실행처럼 run-local.ps1 을 거치지 않아도
 * CHZZK_* 등 로컬 시크릿이 주입되게 한다(dummy-client-id 로 부팅되는 함정 방지).
 * addLast 라서 실제 OS 환경변수/JVM 프로퍼티가 항상 우선한다(운영 systemd env 무영향).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 3 && dir != null; depth++, dir = dir.getParent()) {
            Path envFile = dir.resolve(".env");
            if (Files.isRegularFile(envFile)) {
                Map<String, Object> props = parse(envFile);
                if (!props.isEmpty()) {
                    environment.getPropertySources().addLast(new MapPropertySource("dotenv:" + envFile, props));
                }
                return;
            }
        }
    }

    private Map<String, Object> parse(Path envFile) {
        Map<String, Object> props = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq <= 0) continue;
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.length() >= 2 ? value.substring(1, value.length() - 1) : value;
                }
                props.put(key, value);
            }
        } catch (IOException e) {
            // .env 로드는 편의 기능 — 실패해도 부팅은 계속(기본값/실 env 로 동작)
        }
        return props;
    }
}
