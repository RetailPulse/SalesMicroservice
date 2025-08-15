package com.retailpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SalesMicroservice {
    public static void main(String[] args) {
        SpringApplication.run(SalesMicroservice.class, args);
    }
}