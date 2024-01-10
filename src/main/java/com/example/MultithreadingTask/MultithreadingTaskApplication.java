package com.example.MultithreadingTask;

import com.example.MultithreadingTask.service.MultiThreadingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MultithreadingTaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultithreadingTaskApplication.class, args);
        MultiThreadingService multiThreadingService = new MultiThreadingService();
        multiThreadingService.search("/home/vamshi/Documents", "vamshi", "/home/vamshi/logs/");
    }
}


