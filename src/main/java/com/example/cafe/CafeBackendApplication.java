package com.example.cafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CafeBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(CafeBackendApplication.class, args);
  }

}
