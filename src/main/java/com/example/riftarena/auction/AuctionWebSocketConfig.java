package com.example.riftarena.auction;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class AuctionWebSocketConfig implements WebSocketConfigurer{
 private final AuctionWebSocketHandler handler;
 public AuctionWebSocketConfig(AuctionWebSocketHandler handler){this.handler=handler;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry r){r.addHandler(handler,"/ws/auction").setAllowedOriginPatterns("*");}
}
