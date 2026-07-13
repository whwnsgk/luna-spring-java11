package com.example.riftarena.champion;
import java.util.*;
import org.apache.ibatis.annotations.*;
@Mapper
public interface ChampionMapper{
 List<Map<String,Object>> list();
 Map<String,Object> resolve(@Param("value") String value);
}
