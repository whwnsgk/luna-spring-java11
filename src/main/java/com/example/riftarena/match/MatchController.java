package com.example.riftarena.match;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/matches")
public class MatchController{
 private final MatchService s;public MatchController(MatchService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(){return s.list();}
 @GetMapping("/{id}") public Map<String,Object> one(@PathVariable Long id){return s.detail(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object>p){return s.create(p);}
 @GetMapping("/dashboard") public List<Map<String,Object>> dash(){return s.dashboard();}
 @GetMapping("/ranking") public List<Map<String,Object>> rank(){return s.ranking();}
}
