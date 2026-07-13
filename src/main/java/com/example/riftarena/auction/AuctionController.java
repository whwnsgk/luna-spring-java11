package com.example.riftarena.auction;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auctions")
public class AuctionController{
 private final AuctionService s;public AuctionController(AuctionService s){this.s=s;}
 @PostMapping("/rooms") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object>p){return s.create(p);}
 @GetMapping("/rooms/{code}") public Map<String,Object> get(@PathVariable String code){return s.state(code);}
}
