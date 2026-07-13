package com.example.riftarena.season;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SeasonService {
 private final SeasonMapper mapper;
 public SeasonService(SeasonMapper mapper){this.mapper=mapper;}
 public List<Map<String,Object>> list(){return mapper.list();}
 public Map<String,Object> active(){return mapper.active();}
 @Transactional public Map<String,Object> create(Map<String,Object> p){
  String name=p.get("seasonName")==null?"":String.valueOf(p.get("seasonName")).trim();
  if(name.isEmpty()) throw new IllegalArgumentException("시즌 이름을 입력해주세요.");
  p.put("seasonName",name);
  if(Boolean.TRUE.equals(p.get("activeYn"))) mapper.deactivateAll();
  mapper.insert(p);
  Long id=((Number)p.get("seasonId")).longValue();
  return mapper.find(id);
 }
 @Transactional public Map<String,Object> activate(Long id){
  if(mapper.find(id)==null) throw new IllegalArgumentException("시즌을 찾을 수 없습니다.");
  mapper.deactivateAll();mapper.activate(id);return mapper.find(id);
 }
}
