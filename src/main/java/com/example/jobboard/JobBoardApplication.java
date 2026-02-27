package com.example.jobboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;

@SpringBootApplication(exclude = {MailSenderAutoConfiguration.class})
public class JobBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobBoardApplication.class, args);
    }
}