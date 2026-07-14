package com.example.riftarena.match;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import com.example.riftarena.balance.*;
@RestController@RequestMapping("/api/matches")
public class MatchController{
 private final MatchService s;private final BalanceTuningService balance;
 public MatchController(MatchService s,BalanceTuningService balance){this.s=s;this.balance=balance;}
 @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false) Long seasonId){return s.list(seasonId);}
 @GetMapping("/{id}") public Map<String,Object> one(@PathVariable Long id){return s.detail(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object>p){return s.create(p);}
 @PostMapping("/force") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> force(@RequestBody Map<String,Object>p){p.put("forceYn",true);return s.create(p);}
 @PutMapping("/{id}") public Map<String,Object> update(@PathVariable Long id,@RequestBody Map<String,Object>p){return s.update(id,p);}
 @GetMapping("/{id}/balance-votes") public Map<String,Object> votes(@PathVariable Long id,@RequestParam String voterKey){return balance.voteSummary(id,voterKey);}
 @PostMapping("/{id}/balance-votes") public Map<String,Object> vote(@PathVariable Long id,@RequestBody Map<String,Object>p){return balance.vote(id,p);}
 @GetMapping("/recent-modified") public List<Map<String,Object>> recentModified(){return s.recentModified();}
 @GetMapping("/dashboard") public List<Map<String,Object>> dash(@RequestParam(required=false) Long seasonId){return s.dashboard(seasonId);}
 @GetMapping("/ranking") public List<Map<String,Object>> rank(@RequestParam(required=false) Long seasonId){return s.ranking(seasonId);}
 @GetMapping("/awards") public List<Map<String,Object>> awards(@RequestParam(required=false) Long seasonId){return s.awards(seasonId);}
}
