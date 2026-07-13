package com.example.riftarena.discord;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/discord")
public class DiscordController {
 private final DiscordService service;public DiscordController(DiscordService service){this.service=service;}
 @GetMapping("/status") public Map<String,Object> status(){return Collections.singletonMap("configured",service.configured());}
 @PostMapping("/send") public Map<String,Object> send(@RequestBody Map<String,Object> p){service.send(String.valueOf(p.get("content")));return Collections.singletonMap("sent",true);}
}
