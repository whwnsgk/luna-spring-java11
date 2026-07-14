package com.example.riftarena.balance;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface BalanceTuningMapper{
 Map<String,Object> setting();
 int updateSetting(Map<String,Object> p);
 int upsertVote(Map<String,Object> p);
 Map<String,Object> voteSummary(@Param("matchId") Long matchId,@Param("voterKey") String voterKey);
 List<Map<String,Object>> analysisRows();
 int insertFeature(Map<String,Object> p);
}
