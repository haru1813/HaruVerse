package com.haru.haruverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// HaruVerse 백엔드 진입점. @SpringBootApplication = 자동설정 + 컴포넌트 스캔 + 설정.
@SpringBootApplication
public class HaruverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaruverseApplication.class, args);
    }
}
