package com.example.riftarena.auction;
import java.net.URI;import java.util.*;import org.springframework.stereotype.Component;import org.springframework.web.socket.*;import org.springframework.web.socket.handler.TextWebSocketHandler;import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class AuctionWebSocketHandler extends TextWebSocketHandler{
 private final AuctionService service;private final ObjectMapper om;
 public AuctionWebSocketHandler(AuctionService service,ObjectMapper om){this.service=service;this.om=om;}
 @Override public void afterConnectionEstablished(WebSocketSession s)throws Exception{
  Map<String,String>q=query(s.getUri());
  String room=q.get("roomCode"),role=q.getOrDefault("role","SPECTATOR").toUpperCase();
  String clientId=q.getOrDefault("clientId",s.getId());
  s.getAttributes().put("roomCode",room);s.getAttributes().put("role",role);s.getAttributes().put("clientId",clientId);
  try{
   service.join(room,role,clientId,s);
  }catch(Exception e){
   Map<String,Object>x=new LinkedHashMap<>();x.put("type","JOIN_REJECTED");x.put("role",role);x.put("message",e.getMessage());
   if(s.isOpen())s.sendMessage(new TextMessage(om.writeValueAsString(x)));
   if(s.isOpen())s.close(CloseStatus.POLICY_VIOLATION.withReason("경매방 접속 거절"));
  }
 }
 @Override protected void handleTextMessage(WebSocketSession s,TextMessage m)throws Exception{try{Map<String,Object>p=om.readValue(m.getPayload(),Map.class);service.action((String)s.getAttributes().get("roomCode"),(String)s.getAttributes().get("role"),p);}catch(Exception e){Map<String,Object>x=new LinkedHashMap<>();x.put("type","ERROR");x.put("message",e.getMessage());s.sendMessage(new TextMessage(om.writeValueAsString(x)));}}
 @Override public void afterConnectionClosed(WebSocketSession s,CloseStatus st){service.leave((String)s.getAttributes().get("roomCode"),(String)s.getAttributes().get("role"),s);}
 private Map<String,String>query(URI u){Map<String,String>m=new HashMap<>();if(u==null||u.getQuery()==null)return m;for(String x:u.getQuery().split("&")){String[]a=x.split("=",2);m.put(a[0],a.length>1?java.net.URLDecoder.decode(a[1],java.nio.charset.StandardCharsets.UTF_8):"");}return m;}
}
