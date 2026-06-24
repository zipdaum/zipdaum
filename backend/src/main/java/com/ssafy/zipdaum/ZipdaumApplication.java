package com.ssafy.zipdaum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class ZipdaumApplication {

  public static void main(String[] args) {
    SpringApplication.run(ZipdaumApplication.class, args);
  }

}
