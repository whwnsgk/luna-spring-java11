package com.example.riftarena.match;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface MatchMapper{
 int insertMatch(Map<String,Object>p);int insertPlayer(Map<String,Object>p);int updateMatch(Map<String,Object>p);int deletePlayers(@Param("matchId") Long matchId);int insertChangeLog(Map<String,Object>p);
 Long activeSeasonId();
 List<Map<String,Object>> list(@Param("seasonId") Long seasonId);
 Map<String,Object> find(Long id);List<Map<String,Object>> players(Long id);
 List<Map<String,Object>> dashboard(@Param("seasonId") Long seasonId);
 List<Map<String,Object>> ranking(@Param("seasonId") Long seasonId);
 List<Map<String,Object>> awards(@Param("seasonId") Long seasonId);
 List<Map<String,Object>> recentModified();
}
