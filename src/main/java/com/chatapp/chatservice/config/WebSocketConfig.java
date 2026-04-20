package com.chatapp.chatservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend is point par connect karega (e.g., http://localhost:8080/ws-chat)
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://localhost:4200") // Angular port
                .withSockJS(); // Older browsers support ke liye
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic -> Broadcast (Group chat)
        // /queue -> Private (1-to-1 chat)
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Client se server ko message bhejne ka prefix
        registry.setApplicationDestinationPrefixes("/app");
        
        // User specific messages ke liye
        registry.setUserDestinationPrefix("/user");
    }
}