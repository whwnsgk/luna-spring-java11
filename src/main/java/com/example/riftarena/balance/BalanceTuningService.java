package com.example.riftarena.balance;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceTuningService{
 private final BalanceTuningMapper mapper;
 public BalanceTuningService(BalanceTuningMapper mapper){this.mapper=mapper;}

 public Map<String,Object> setting(){
  Map<String,Object> value=mapper.setting();
  return value==null?defaults():value;
 }

 @Transactional public Map<String,Object> updateSetting(Map<String,Object> p){
  Map<String,Object> current=new LinkedHashMap<>(setting());
  for(String key:Arrays.asList("matchupDiffWeight","teamDiffWeight","skill1Penalty","skill3Penalty","crowdingPenalty","topTierSupportPenalty")){
   if(p!=null&&p.get(key)!=null)current.put(key,p.get(key));
  }
  mapper.updateSetting(current);
  return setting();
 }

 @Transactional public Map<String,Object> vote(Long matchId,Map<String,Object> p){
  String value=String.valueOf(p.get("voteValue")).toUpperCase(Locale.ROOT);
  if(!Arrays.asList("PERFECT","NORMAL","BAD").contains(value))throw new IllegalArgumentException("잘못된 밸런스 평가입니다.");
  String voterKey=String.valueOf(p.get("voterKey")).trim();
  if(voterKey.isEmpty())throw new IllegalArgumentException("투표 식별값이 없습니다.");
  Map<String,Object> row=new HashMap<>();row.put("matchId",matchId);row.put("voterKey",voterKey);row.put("voteValue",value);
  mapper.upsertVote(row);
  return voteSummary(matchId,voterKey);
 }

 public Map<String,Object> voteSummary(Long matchId,String voterKey){
  Map<String,Object> result=mapper.voteSummary(matchId,voterKey);
  if(result==null){result=new LinkedHashMap<>();result.put("perfectCount",0);result.put("normalCount",0);result.put("badCount",0);result.put("myVote",null);}
  return result;
 }

 public Map<String,Object> analysis(){
  List<Map<String,Object>> rows=mapper.analysisRows();
  Map<String,Object> current=setting();
  Map<String,Object> recommended=new LinkedHashMap<>(current);
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("current",current);result.put("sampleCount",rows.size());

  if(rows.size()<5){
   result.put("recommended",recommended);
   result.put("message","투표가 5경기 이상 쌓이면 가중치 추천을 시작합니다.");
   return result;
  }

  double goodMatch=0,badMatch=0,goodTeam=0,badTeam=0,goodS1=0,badS1=0,goodS3=0,badS3=0;
  int goodN=0,badN=0;
  for(Map<String,Object> row:rows){
   double score=num(row.get("voteScore"));
   if(score>=2.5){goodN++;goodMatch+=num(row.get("matchupDiff"));goodTeam+=num(row.get("teamDiff"));goodS1+=num(row.get("skill1Count"));goodS3+=num(row.get("skill3Count"));}
   if(score<=1.5){badN++;badMatch+=num(row.get("matchupDiff"));badTeam+=num(row.get("teamDiff"));badS1+=num(row.get("skill1Count"));badS3+=num(row.get("skill3Count"));}
  }
  if(goodN>0&&badN>0){
   recommended.put("matchupDiffWeight",tune(num(current.get("matchupDiffWeight")),badMatch/badN,goodMatch/goodN));
   recommended.put("teamDiffWeight",tune(num(current.get("teamDiffWeight")),badTeam/badN,goodTeam/goodN));
   recommended.put("skill1Penalty",tune(num(current.get("skill1Penalty")),badS1/badN,goodS1/goodN));
   recommended.put("skill3Penalty",tune(num(current.get("skill3Penalty")),badS3/badN,goodS3/goodN));
   result.put("message","황밸 경기와 나빠요 경기의 특성 차이를 기준으로 추천했습니다.");
  }else result.put("message","황밸과 나빠요 표본이 모두 있어야 추천값이 조정됩니다.");
  result.put("recommended",recommended);
  return result;
 }

 @Transactional public void saveFeature(Long matchId,Object raw){
  if(!(raw instanceof Map))return;
  @SuppressWarnings("unchecked") Map<String,Object> source=(Map<String,Object>)raw;
  Map<String,Object> row=new HashMap<>(source);row.put("matchId",matchId);
  mapper.insertFeature(row);
 }

 private Map<String,Object> defaults(){
  Map<String,Object> d=new LinkedHashMap<>();
  d.put("matchupDiffWeight",1.0);d.put("teamDiffWeight",0.35);d.put("skill1Penalty",120.0);d.put("skill3Penalty",300.0);d.put("crowdingPenalty",50.0);d.put("topTierSupportPenalty",1000000.0);
  return d;
 }
 private double tune(double current,double bad,double good){
  if(bad<=good)return current;
  double ratio=Math.min(1.30,1.0+(bad-good)/Math.max(1.0,good)*0.10);
  return Math.round(current*ratio*1000.0)/1000.0;
 }
 private double num(Object o){return o instanceof Number?((Number)o).doubleValue():0.0;}
}
