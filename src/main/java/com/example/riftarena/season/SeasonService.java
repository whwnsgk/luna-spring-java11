package com.example.riftarena.season;
import java.util.*;import java.time.*;import java.time.format.DateTimeParseException;
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

  LocalDate startDate=parseDate(p.get("startDate"),"시작일",true);
  LocalDate endDate=parseDate(p.get("endDate"),"종료일",false);
  if(endDate!=null&&endDate.isBefore(startDate)){
   throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
  }

  boolean activeYn=Boolean.TRUE.equals(p.get("activeYn"));
  if(mapper.active()==null)activeYn=true;

  Map<String,Object> param=new HashMap<>();
  param.put("seasonName",name);
  param.put("startDate",startDate);
  param.put("endDate",endDate);
  param.put("activeYn",activeYn);

  if(activeYn)mapper.deactivateAll();
  mapper.insert(param);

  Object rawId=param.get("seasonId");
  if(!(rawId instanceof Number))throw new IllegalStateException("시즌 ID 생성에 실패했습니다.");
  return mapper.find(((Number)rawId).longValue());
 }

 private LocalDate parseDate(Object value,String label,boolean required){
  String text=value==null?"":String.valueOf(value).trim();
  if(text.isEmpty()){
   if(required)throw new IllegalArgumentException(label+"을 입력해주세요.");
   return null;
  }
  try{return LocalDate.parse(text);}
  catch(DateTimeParseException e){throw new IllegalArgumentException(label+" 형식이 올바르지 않습니다.");}
 }
 @Transactional public Map<String,Object> activate(Long id){
  if(mapper.find(id)==null) throw new IllegalArgumentException("시즌을 찾을 수 없습니다.");
  mapper.deactivateAll();mapper.activate(id);return mapper.find(id);
 }
}
