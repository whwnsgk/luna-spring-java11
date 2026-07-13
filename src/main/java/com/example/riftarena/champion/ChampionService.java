package com.example.riftarena.champion;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public class ChampionService{
 private final ChampionMapper mapper;
 public ChampionService(ChampionMapper mapper){this.mapper=mapper;}
 public List<Map<String,Object>> list(){return mapper.list();}
 public Map<String,Object> resolve(String value){
  String v=value==null?"":value.trim();
  if(v.isEmpty())throw new IllegalArgumentException("챔피언을 선택해주세요.");
  Map<String,Object> c=mapper.resolve(v);
  if(c==null)throw new IllegalArgumentException("'"+v+"'은(는) 등록되지 않은 챔피언입니다. 자동완성 목록에서 선택해주세요.");
  return c;
 }
}
