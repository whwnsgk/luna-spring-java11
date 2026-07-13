package com.example.riftarena.match;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/matches")
public class MatchController{
 private final MatchService s;public MatchController(MatchService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) Long seasonId){return s.list(seasonId);}
 @GetMapping("/{id}") public Map<String,Object> one(@PathVariable Long id){return s.detail(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object>p){return s.create(p);}
 @GetMapping("/dashboard") public List<Map<String,Object>> dash(@RequestParam(required=false) Long seasonId){return s.dashboard(seasonId);}
 @GetMapping("/ranking") public List<Map<String,Object>> rank(@RequestParam(required=false) Long seasonId){return s.ranking(seasonId);}
 @GetMapping("/awards") public List<Map<String,Object>> awards(@RequestParam(required=false) Long seasonId){return s.awards(seasonId);}
}
