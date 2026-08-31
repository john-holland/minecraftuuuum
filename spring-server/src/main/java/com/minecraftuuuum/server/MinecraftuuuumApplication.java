package com.minecraftuuuum.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MinecraftuuuumApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinecraftuuuumApplication.class, args);
    }
}
