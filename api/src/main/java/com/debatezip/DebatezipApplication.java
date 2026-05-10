package com.debatezip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 메인 클래스가 com.debatezip 패키지에 위치하므로,
// 기본 ComponentScan / EntityScan / JpaRepository 스캔이 com.debatezip.* 전체를 커버한다.
@SpringBootApplication
public class DebatezipApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebatezipApplication.class, args);
    }
}
