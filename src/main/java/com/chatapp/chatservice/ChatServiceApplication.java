package com.chatapp.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct; // Import add karna mat bhoolna
import java.util.TimeZone;           
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChatServiceApplication {

    // 🛑 NAYA FIX: App start hote hi Timezone IST set kar dega
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}