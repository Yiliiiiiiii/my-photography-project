package com.yiliiii.project.my_photography_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MyPhotographyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyPhotographyProjectApplication.class, args);
    }
}
