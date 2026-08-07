package com.couple.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.couple.app.mapper")
public class CoupleApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoupleApplication.class, args);
    }
}
