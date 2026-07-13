package com.example.riftarena.discord;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
@Service
public class DiscordService {
 @Value("${discord.webhook.url:}") private String webhookUrl;
 private final RestTemplate restTemplate=new RestTemplate();
 public boolean configured(){return StringUtils.hasText(webhookUrl);}
 public void send(String content){
  if(!configured()) throw new IllegalStateException("DISCORD_WEBHOOK_URL이 설정되지 않았습니다.");
  if(content==null||content.trim().isEmpty()) throw new IllegalArgumentException("전송할 내용이 없습니다.");
  if(content.length()>1900) content=content.substring(0,1900)+"\n…";
  Map<String,Object> body=new LinkedHashMap<>();body.put("content",content);
  HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);
  restTemplate.exchange(webhookUrl,HttpMethod.POST,new HttpEntity<Map<String,Object>>(body,h),String.class);
 }
}
