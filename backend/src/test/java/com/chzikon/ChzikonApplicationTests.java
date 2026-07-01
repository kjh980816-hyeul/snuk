package com.chzikon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 컨텍스트 로드 = Flyway 마이그레이션 + JPA ddl-auto:validate + 빈 구성 검증. */
@SpringBootTest
class ChzikonApplicationTests {

    @Test
    void contextLoads() {
    }
}
