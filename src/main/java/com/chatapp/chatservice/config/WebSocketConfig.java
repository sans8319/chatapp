package com.chatapp.chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

    // 🛑 FIX 1: STOMP Protocol ki limits badhane ke liye isko UNCOMMENT kar diya
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(50 * 1024 * 1024);      // 50 MB Input limit
        registration.setSendTimeLimit(60000);                    // 60 seconds
        registration.setSendBufferSizeLimit(50 * 1024 * 1024);   // 50 MB Output buffer limit
    }

    // 🛑 FIX 2: Tomcat Server ki hidden 8KB limit ko bypass karne ke liye naya Bean add kiya
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Raw WebSocket Buffer ko badha kar 50MB kar diya taaki lamba code aaram se aa sake
        container.setMaxTextMessageBufferSize(50 * 1024 * 1024);   // 50 MB for Text/Code
        container.setMaxBinaryMessageBufferSize(50 * 1024 * 1024); // 50 MB for Media
        return container;
    }
}