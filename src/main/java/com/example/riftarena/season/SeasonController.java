package com.example.riftarena.season;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/seasons")
public class SeasonController {
 private final SeasonService service;
 public SeasonController(SeasonService service){this.service=service;}
 @GetMapping public List<Map<String,Object>> list(){return service.list();}
 @GetMapping("/active") public Map<String,Object> active(){Map<String,Object> a=service.active();return a==null?Collections.emptyMap():a;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object> p){return service.create(p);}
 @PutMapping("/{id}/activate") public Map<String,Object> activate(@PathVariable Long id){return service.activate(id);}
}
