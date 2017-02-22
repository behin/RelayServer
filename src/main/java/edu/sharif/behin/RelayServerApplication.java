package edu.sharif.behin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Created by Behin on 2/22/2017.
 */
@SpringBootApplication
public class RelayServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RelayServerApplication.class, args);
    }

    @Configuration
    @EnableWebSocket
    public static class WebSocketConfig implements WebSocketConfigurer {

        @Autowired
        private WebSocketHandler webSocketHandler;

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(webSocketHandler, WebSocketHandler.URI_TEMPLATE);
        }

        @Bean
        public ServletServerContainerFactoryBean createWebSocketContainer() {
            ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
            container.setMaxSessionIdleTimeout(1000 * 60 * 30); // 30 minutes
            container.setMaxBinaryMessageBufferSize(512*1024);
            return container;
        }
    }
}
