package com.ssafy.zipdaum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableScheduling
public class ZipdaumApplication {

  public static void main(String[] args) {
    SpringApplication.run(ZipdaumApplication.class, args);
  }

}
