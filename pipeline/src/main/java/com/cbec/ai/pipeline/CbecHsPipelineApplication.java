package com.cbec.ai.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class CbecHsPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbecHsPipelineApplication.class, args);
    }
}
