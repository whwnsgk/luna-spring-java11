package com.example.riftarena.member;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/members")
public class MemberController {
 private final MemberService service; public MemberController(MemberService service){this.service=service;}
 @GetMapping public List<Map<String,Object>> list(){return service.list();}
 @GetMapping("/{id}") public Map<String,Object> one(@PathVariable Long id,@RequestParam(required=false) Long seasonId){return service.detail(id,seasonId);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object> p){return service.save(p,null);}
 @PutMapping("/{id}") public Map<String,Object> update(@PathVariable Long id,@RequestBody Map<String,Object> p){return service.save(p,id);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
 @PostMapping("/{id}/riot-refresh") public Map<String,Object> refresh(@PathVariable Long id){return service.refreshRiot(id);}
 @PostMapping("/riot-refresh-all") public Map<String,Object> refreshAll(){return service.refreshAllRiot();}
 @GetMapping("/hall-of-fame") public List<Map<String,Object>> hall(@RequestParam(required=false) Long seasonId){return service.hall(seasonId);}
 @GetMapping("/riot/status") public Map<String,Object> status(){return Collections.singletonMap("configured",service.riotConfigured());}
 @GetMapping("/riot/lookup") public Map<String,Object> lookup(@RequestParam String gameName,@RequestParam String tagLine){return service.lookup(gameName,tagLine);}
}
