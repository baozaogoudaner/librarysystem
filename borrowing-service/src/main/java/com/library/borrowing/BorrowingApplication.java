package com.library.borrowing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.library.borrowing", "com.library.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.library.borrowing.feign")
@EnableScheduling
public class BorrowingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BorrowingApplication.class, args);
    }
}
