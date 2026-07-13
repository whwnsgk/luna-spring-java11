package com.example.riftarena.member;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
@RestController
@RequestMapping("/api/members")
public class MemberController {
 private final MemberService service;
 @Value("${riot.api.key:}") private String riotKey;
 public MemberController(MemberService service){this.service=service;}
 @GetMapping public List<Map<String,Object>> list(){return service.list();}
 @GetMapping("/{id}") public Map<String,Object> one(@PathVariable Long id){return service.find(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object> p){return service.save(p,null);}
 @PutMapping("/{id}") public Map<String,Object> update(@PathVariable Long id,@RequestBody Map<String,Object> p){return service.save(p,id);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
 @GetMapping("/hall-of-fame") public List<Map<String,Object>> hall(){return service.hall();}
 @GetMapping("/riot/status") public Map<String,Object> status(){return Collections.singletonMap("configured",riotKey!=null&&!riotKey.isBlank());}
 @GetMapping("/riot/lookup")
 public Map lookup(@RequestParam String gameName,@RequestParam String tagLine){
  if(riotKey==null||riotKey.isBlank()) throw new IllegalStateException("RIOT_API_KEY가 없습니다. 수동 등록은 가능합니다.");
  String url="https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"+
   URLEncoder.encode(gameName,StandardCharsets.UTF_8)+"/"+URLEncoder.encode(tagLine,StandardCharsets.UTF_8);
  HttpHeaders h=new HttpHeaders();h.set("X-Riot-Token",riotKey);
  return new RestTemplate().exchange(URI.create(url),HttpMethod.GET,new HttpEntity<Void>(h),Map.class).getBody();
 }
}
