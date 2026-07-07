package com.library.seat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.library.seat.mapper")
@ComponentScan(basePackages = {"com.library.seat", "com.library.common"})
public class SeatApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatApplication.class, args);
    }
}
