package com.example.riftarena.rating;

import java.util.*;

/**
 * 선택된 10명의 1순위 포지션 분포를 기준으로 그 내전에서만 사용할 유효 레이팅을 계산합니다.
 * DB의 member.balance_score는 변경하지 않습니다.
 */
public final class PositionCrowdingAdjuster {
 private static final Set<String> LANES=new LinkedHashSet<>(Arrays.asList("TOP","JUNGLE","MID","ADC","SUPPORT"));
 private static final int REQUIRED_PER_LANE=2;
 private static final int PENALTY_PER_EXCESS=50;

 private PositionCrowdingAdjuster(){}

 public static List<Map<String,Object>> adjust(List<Map<String,Object>> members){
  List<Map<String,Object>> result=new ArrayList<>();
  Map<String,Integer> primaryCounts=new LinkedHashMap<>();
  for(String lane:LANES)primaryCounts.put(lane,0);

  for(Map<String,Object> member:members){
   String primary=primaryPosition(member);
   if(LANES.contains(primary))primaryCounts.put(primary,primaryCounts.get(primary)+1);
  }

  for(Map<String,Object> source:members){
   Map<String,Object> member=new LinkedHashMap<>(source);
   int original=number(source.get("balanceScore"),1500);
   String primary=primaryPosition(source);
   int excess=LANES.contains(primary)?Math.max(0,primaryCounts.get(primary)-REQUIRED_PER_LANE):0;
   int basePenalty=excess*PENALTY_PER_EXCESS;
   double multiplier=penaltyMultiplier(source,primary);
   int penalty=(int)Math.round(basePenalty*multiplier);
   int effective=Math.max(600,original-penalty);

   member.put("originalBalanceScore",original);
   member.put("positionCrowdingPenalty",penalty);
   member.put("positionCrowdingPosition",primary);
   member.put("positionCrowdingCount",LANES.contains(primary)?primaryCounts.get(primary):0);
   member.put("effectiveBalanceScore",effective);
   // 기존 화면과 경매 로직이 balanceScore를 읽으므로 방/조합 내부 복사본만 유효 점수로 교체합니다.
   member.put("balanceScore",effective);
   result.add(member);
  }
  return result;
 }

 public static Map<String,Integer> primaryCounts(List<Map<String,Object>> members){
  Map<String,Integer> counts=new LinkedHashMap<>();
  for(String lane:LANES)counts.put(lane,0);
  for(Map<String,Object> member:members){
   String primary=primaryPosition(member);
   if(counts.containsKey(primary))counts.put(primary,counts.get(primary)+1);
  }
  return counts;
 }

 @SuppressWarnings("unchecked")
 private static String primaryPosition(Map<String,Object> member){
  Object raw=member.get("preferredPositions");
  if(!(raw instanceof List))return "";
  List<Object> positions=(List<Object>)raw;
  for(Object value:positions){
   String position=String.valueOf(value).trim().toUpperCase(Locale.ROOT);
   if(LANES.contains(position))return position;
   if("FILL".equals(position))return "FILL";
  }
  return "";
 }

 @SuppressWarnings("unchecked")
 private static double penaltyMultiplier(Map<String,Object> member,String primary){
  Object raw=member.get("preferredPositions");
  if(!(raw instanceof List))return 1.0;
  List<Object> positions=(List<Object>)raw;
  boolean fill=false;
  boolean alternative=false;
  for(Object value:positions){
   String position=String.valueOf(value).trim().toUpperCase(Locale.ROOT);
   if("FILL".equals(position))fill=true;
   else if(LANES.contains(position)&&!position.equals(primary))alternative=true;
  }
  if(fill)return 0.30;
  if(alternative)return 0.70;
  return 1.0;
 }

 private static int number(Object value,int fallback){
  return value instanceof Number?((Number)value).intValue():fallback;
 }
}
