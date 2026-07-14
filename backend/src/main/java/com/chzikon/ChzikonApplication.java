package com.chzikon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling // 후기 마감 경고 스윕(ReviewDeadlineService)
public class ChzikonApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChzikonApplication.class, args);
    }
}
