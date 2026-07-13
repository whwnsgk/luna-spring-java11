package com.example.riftarena.season;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface SeasonMapper {
 List<Map<String,Object>> list();
 Map<String,Object> active();
 int insert(Map<String,Object> p);
 int deactivateAll();
 int activate(Long seasonId);
 Map<String,Object> find(Long seasonId);
}
