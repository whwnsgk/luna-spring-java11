package com.example.riftarena.match;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface MatchMapper{
 int insertMatch(Map<String,Object>p);int insertPlayer(Map<String,Object>p);
 List<Map<String,Object>> list();Map<String,Object> find(Long id);List<Map<String,Object>> players(Long id);
 List<Map<String,Object>> dashboard();List<Map<String,Object>> ranking();
}
