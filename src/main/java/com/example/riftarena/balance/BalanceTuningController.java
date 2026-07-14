package com.example.riftarena.balance;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/balance-tuning")
public class BalanceTuningController{
 private final BalanceTuningService service;
 public BalanceTuningController(BalanceTuningService service){this.service=service;}
 @GetMapping("/setting") public Map<String,Object> setting(){return service.setting();}
 @PutMapping("/setting") public Map<String,Object> update(@RequestBody Map<String,Object> p){return service.updateSetting(p);}
 @GetMapping("/analysis") public Map<String,Object> analysis(){return service.analysis();}
 @PostMapping("/analysis/apply") public Map<String,Object> apply(){Map<String,Object>a=service.analysis();@SuppressWarnings("unchecked")Map<String,Object>r=(Map<String,Object>)a.get("recommended");return service.updateSetting(r);}
}
