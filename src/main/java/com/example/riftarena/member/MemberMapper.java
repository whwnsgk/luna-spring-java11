package com.example.riftarena.member;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface MemberMapper {
 List<Map<String,Object>> list();
 Map<String,Object> find(Long memberId);
 int duplicate(@Param("gameName") String gameName,@Param("tagLine") String tagLine,@Param("excludeId") Long excludeId);
 int insert(Map<String,Object> p);
 int update(Map<String,Object> p);
 int deactivate(Long memberId);
 int deletePositions(Long memberId);
 int insertPosition(@Param("memberId")Long memberId,@Param("positionCode")String positionCode,@Param("priorityNo")int priorityNo);
 List<Map<String,Object>> byIds(@Param("ids") List<Long> ids);
 List<Map<String,Object>> hallOfFame();
}
