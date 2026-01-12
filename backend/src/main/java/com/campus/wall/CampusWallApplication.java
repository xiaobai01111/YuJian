package com.campus.wall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campus.wall.mapper.*")
@EnableScheduling
public class CampusWallApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusWallApplication.class, args);
    }

}
