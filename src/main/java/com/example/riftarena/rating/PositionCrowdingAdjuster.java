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
  Map<String,Integer> capableLaneCounts=capableLaneCounts(members);

  for(Map<String,Object> source:members){
   Map<String,Object> member=new LinkedHashMap<>(source);
   int original=number(source.get("balanceScore"),1500);
   Map<String,Integer> capableLanes=capablePositions(source);

   String appliedLane="";
   int appliedLaneCount=0;
   int appliedPreference=0;
   int maxPenalty=0;

   /*
    * 2점(솔랭 가능)과 3점(다이아 이상 주라인)을 모두 라인 과밀 인원으로 집계합니다.
    * 한 사람이 여러 라인을 2/3점으로 설정했다면 전부 검사하고,
    * 가장 큰 과밀 페널티 하나만 개인 유효 레이팅에 적용합니다.
    */
   for(Map.Entry<String,Integer> entry:capableLanes.entrySet()){
    String lane=entry.getKey();
    int preference=entry.getValue();
    int count=capableLaneCounts.getOrDefault(lane,0);
    int excess=Math.max(0,count-REQUIRED_PER_LANE);
    int basePenalty=excess*PENALTY_PER_EXCESS;

    // 3점은 강한 주라인이므로 전액, 2점은 솔랭 가능 라인이므로 70% 적용합니다.
    double ownLaneMultiplier=preference==3?1.0:0.70;
    int candidatePenalty=(int)Math.round(basePenalty*ownLaneMultiplier);

    // 다른 대체 라인이 있으면 페널티를 추가 완화합니다.
    candidatePenalty=(int)Math.round(candidatePenalty*alternativeMultiplier(source,lane));

    if(candidatePenalty>maxPenalty){
     maxPenalty=candidatePenalty;
     appliedLane=lane;
     appliedLaneCount=count;
     appliedPreference=preference;
    }
   }

   int effective=Math.max(0,original-maxPenalty);

   member.put("originalBalanceScore",original);
   member.put("positionCrowdingPenalty",maxPenalty);
   member.put("positionCrowdingPosition",appliedLane);
   member.put("positionCrowdingCount",appliedLaneCount);
   member.put("positionCrowdingPreference",appliedPreference);
   member.put("positionCrowdingCapablePositions",new LinkedHashMap<>(capableLanes));
   member.put("positionCrowdingCapableLaneCounts",new LinkedHashMap<>(capableLaneCounts));
   member.put("effectiveBalanceScore",effective);
   member.put("balanceScore",effective);
   result.add(member);
  }
  return result;
 }

 /**
  * 2점 또는 3점인 라인을 모두 집계합니다.
  */
 public static Map<String,Integer> capableLaneCounts(List<Map<String,Object>> members){
  Map<String,Integer> counts=new LinkedHashMap<>();
  for(String lane:LANES)counts.put(lane,0);

  for(Map<String,Object> member:members){
   for(String lane:capablePositions(member).keySet()){
    counts.put(lane,counts.get(lane)+1);
   }
  }
  return counts;
 }

 public static Map<String,Integer> mainLaneCounts(List<Map<String,Object>> members){
  return capableLaneCounts(members);
 }

 public static Map<String,Integer> primaryCounts(List<Map<String,Object>> members){
  return capableLaneCounts(members);
 }

 @SuppressWarnings("unchecked")
 private static Map<String,Integer> capablePositions(Map<String,Object> member){
  Map<String,Integer> result=new LinkedHashMap<>();
  Object raw=member.get("laneProfiles");
  if(!(raw instanceof List))return result;

  for(Object value:(List<Object>)raw){
   if(!(value instanceof Map))continue;
   Map<String,Object> profile=(Map<String,Object>)value;
   String position=String.valueOf(profile.get("positionCode")).trim().toUpperCase(Locale.ROOT);
   int preference=number(profile.get("preferenceScore"),0);

   if(LANES.contains(position)&&preference>=2){
    result.put(position,preference);
   }
  }
  return result;
 }

 @SuppressWarnings("unchecked")
 private static double alternativeMultiplier(Map<String,Object> member,String crowdedLane){
  Object raw=member.get("laneProfiles");
  if(!(raw instanceof List))return 1.0;

  boolean hasOtherThree=false;
  boolean hasOtherTwo=false;
  boolean hasOtherOne=false;

  for(Object value:(List<Object>)raw){
   if(!(value instanceof Map))continue;
   Map<String,Object> profile=(Map<String,Object>)value;
   String position=String.valueOf(profile.get("positionCode")).trim().toUpperCase(Locale.ROOT);
   int preference=number(profile.get("preferenceScore"),0);

   if(!LANES.contains(position)||position.equals(crowdedLane))continue;
   if(preference==3)hasOtherThree=true;
   else if(preference==2)hasOtherTwo=true;
   else if(preference==1)hasOtherOne=true;
  }

  if(hasOtherThree)return 0.40;
  if(hasOtherTwo)return 0.50;
  if(hasOtherOne)return 0.70;
  return 1.0;
 }

 private static int number(Object value,int fallback){
  return value instanceof Number?((Number)value).intValue():fallback;
 }
}
