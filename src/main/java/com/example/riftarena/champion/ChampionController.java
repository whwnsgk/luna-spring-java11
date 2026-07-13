package com.example.riftarena.champion;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/champions")
public class ChampionController{
 private final ChampionService service;
 public ChampionController(ChampionService service){this.service=service;}
 @GetMapping public List<Map<String,Object>> list(){return service.list();}
}
