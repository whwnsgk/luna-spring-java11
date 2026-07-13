package com.example.riftarena.draft;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.example.riftarena.member.*;
import com.example.riftarena.rating.PositionCrowdingAdjuster;

@RestController
@RequestMapping("/api/team-balance")
public class TeamController {
 private static final String[] LANES={"TOP","JUNGLE","MID","ADC","SUPPORT"};
 private static final List<int[]> PERMS=buildPermutations();
 private final MemberService members;

 public TeamController(MemberService members){this.members=members;}

 @SuppressWarnings("unchecked")
 @PostMapping("/auto")
 public Map<String,Object> auto(@RequestBody Map<String,Object> req){
  List<Number> raw=(List<Number>)req.get("memberIds");
  if(raw==null||raw.size()!=10)throw new IllegalArgumentException("정확히 10명이 필요합니다.");

  List<Long> ids=new ArrayList<>();
  for(Number n:raw)ids.add(n.longValue());
  if(new HashSet<>(ids).size()!=10)throw new IllegalArgumentException("중복 멤버가 있습니다.");

  List<Map<String,Object>> original=members.byIds(ids);
  if(original.size()!=10)throw new IllegalArgumentException("멤버 조회에 실패했습니다.");
  List<Map<String,Object>> list=PositionCrowdingAdjuster.adjust(original);
  Map<String,Integer> crowdingCounts=PositionCrowdingAdjuster.primaryCounts(original);

  Candidate best=null;
  // 10명 중 5명 조합 252개 중 대칭 제거: 첫 멤버가 BLUE인 126개만 검사
  for(int mask=0;mask<(1<<10);mask++){
   if(Integer.bitCount(mask)!=5||(mask&1)==0)continue;

   List<Map<String,Object>> blue=new ArrayList<>();
   List<Map<String,Object>> red=new ArrayList<>();
   for(int i=0;i<10;i++)((mask&(1<<i))!=0?blue:red).add(list.get(i));

   LaneAssignment blueAssignment=bestLaneAssignment(blue);
   LaneAssignment redAssignment=bestLaneAssignment(red);
   int blueCost=cost(blue);
   int redCost=cost(red);
   double score=(blueAssignment.fit+redAssignment.fit)*50.0-Math.abs(blueCost-redCost);

   if(best==null||score>best.score){
    best=new Candidate(
      withAssignedLanes(blue,blueAssignment),
      withAssignedLanes(red,redAssignment),
      blueCost,redCost,score,blueAssignment.fit,redAssignment.fit
    );
   }
  }

  return result(
    best.blue,best.red,best.blueCost,best.redCost,
    crowdingMessage(crowdingCounts)+" · 126개 팀 조합과 팀별 120개 라인 배치를 전수 검사했습니다.",
    best.blueFit,best.redFit
  );
 }

 @SuppressWarnings("unchecked")
 @PostMapping("/evaluate")
 public Map<String,Object> eval(@RequestBody Map<String,Object> req){
  List<Long> blueIds=longs((List<Number>)req.get("blueMemberIds"));
  List<Long> redIds=longs((List<Number>)req.get("redMemberIds"));
  if(blueIds.size()!=5||redIds.size()!=5)throw new IllegalArgumentException("각 팀은 5명이어야 합니다.");

  List<Map<String,Object>> originalBlue=members.byIds(blueIds);
  List<Map<String,Object>> originalRed=members.byIds(redIds);
  List<Map<String,Object>> all=new ArrayList<>();all.addAll(originalBlue);all.addAll(originalRed);
  List<Map<String,Object>> adjusted=PositionCrowdingAdjuster.adjust(all);
  Map<Long,Map<String,Object>> byId=new HashMap<>();
  for(Map<String,Object> member:adjusted)byId.put(((Number)member.get("memberId")).longValue(),member);
  List<Map<String,Object>> blue=new ArrayList<>(),red=new ArrayList<>();
  for(Long id:blueIds)blue.add(byId.get(id));
  for(Long id:redIds)red.add(byId.get(id));
  Map<String,Integer> crowdingCounts=PositionCrowdingAdjuster.primaryCounts(all);
  LaneAssignment blueAssignment=bestLaneAssignment(blue);
  LaneAssignment redAssignment=bestLaneAssignment(red);

  return result(
    withAssignedLanes(blue,blueAssignment),
    withAssignedLanes(red,redAssignment),
    cost(blue),cost(red),
    crowdingMessage(crowdingCounts)+" · 현재 수동 팀을 기준으로 최적 라인 배치를 계산했습니다.",
    blueAssignment.fit,redAssignment.fit
  );
 }

 private LaneAssignment bestLaneAssignment(List<Map<String,Object>> team){
  LaneAssignment best=null;
  for(int[] permutation:PERMS){
   int fit=0;
   for(int i=0;i<5;i++)fit+=skill(team.get(i),LANES[permutation[i]]);
   if(best==null||fit>best.fit)best=new LaneAssignment(permutation,fit);
  }
  return best;
 }

 private List<Map<String,Object>> withAssignedLanes(List<Map<String,Object>> team,LaneAssignment assignment){
  List<Map<String,Object>> result=new ArrayList<>();
  for(int i=0;i<team.size();i++){
   Map<String,Object> member=new LinkedHashMap<>(team.get(i));
   member.put("assignedPosition",LANES[assignment.permutation[i]]);
   result.add(member);
  }
  result.sort(Comparator.comparingInt(m->laneOrder(String.valueOf(m.get("assignedPosition")))));
  return result;
 }

 @SuppressWarnings("unchecked")
 private int skill(Map<String,Object> member,String lane){
  Object raw=member.get("laneProfiles");
  if(!(raw instanceof List))return 0;
  for(Object value:(List<Object>)raw){
   if(!(value instanceof Map))continue;
   Map<String,Object> profile=(Map<String,Object>)value;
   String position=String.valueOf(profile.get("positionCode")).toUpperCase(Locale.ROOT);
   if(lane.equals(position)){
    Object score=profile.get("preferenceScore");
    return score instanceof Number?((Number)score).intValue():0;
   }
  }
  return 0;
 }

 private int cost(List<Map<String,Object>> team){
  int sum=0;
  for(Map<String,Object> member:team){
   Object raw=member.get("balanceScore");
   sum+=raw instanceof Number?((Number)raw).intValue():1500;
  }
  return sum;
 }

 private Map<String,Object> result(
   List<Map<String,Object>> blue,List<Map<String,Object>> red,
   int blueCost,int redCost,String message,int blueFit,int redFit
 ){
  double blueExpected=1.0/(1.0+Math.pow(10.0,(redCost-blueCost)/400.0));
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("blueTeam",blue);
  result.put("redTeam",red);
  result.put("blueCost",blueCost);
  result.put("redCost",redCost);
  result.put("costDifference",Math.abs(blueCost-redCost));
  result.put("blueExpectedWinRate",Math.round(blueExpected*1000.0)/10.0);
  result.put("redExpectedWinRate",Math.round((1.0-blueExpected)*1000.0)/10.0);
  result.put("blueLaneFit",blueFit);
  result.put("redLaneFit",redFit);
  result.put("algorithmMessage",message);
  return result;
 }

 private String crowdingMessage(Map<String,Integer> counts){
  List<String> crowded=new ArrayList<>();
  for(String lane:LANES){
   int count=counts.getOrDefault(lane,0);
   if(count>2)crowded.add(lane+" "+count+"명");
  }
  return crowded.isEmpty()?"포지션 과밀 보정 없음":"포지션 과밀 보정: "+String.join(", ",crowded);
 }

 private List<Long> longs(List<Number> values){
  List<Long> result=new ArrayList<>();
  if(values!=null)for(Number value:values)result.add(value.longValue());
  return result;
 }

 private static int laneOrder(String lane){
  for(int i=0;i<LANES.length;i++)if(LANES[i].equals(lane))return i;
  return LANES.length;
 }

 private static List<int[]> buildPermutations(){
  List<int[]> result=new ArrayList<>();
  permute(new int[]{0,1,2,3,4},0,result);
  return result;
 }

 private static void permute(int[] values,int index,List<int[]> result){
  if(index==values.length){
   result.add(Arrays.copyOf(values,values.length));
   return;
  }
  for(int i=index;i<values.length;i++){
   int temp=values[index];values[index]=values[i];values[i]=temp;
   permute(values,index+1,result);
   temp=values[index];values[index]=values[i];values[i]=temp;
  }
 }

 static class LaneAssignment{
  int[] permutation;
  int fit;
  LaneAssignment(int[] permutation,int fit){
   this.permutation=Arrays.copyOf(permutation,permutation.length);
   this.fit=fit;
  }
 }

 static class Candidate{
  List<Map<String,Object>> blue,red;
  int blueCost,redCost,blueFit,redFit;
  double score;
  Candidate(
    List<Map<String,Object>> blue,List<Map<String,Object>> red,
    int blueCost,int redCost,double score,int blueFit,int redFit
  ){
   this.blue=blue;this.red=red;this.blueCost=blueCost;this.redCost=redCost;
   this.score=score;this.blueFit=blueFit;this.redFit=redFit;
  }
 }
}
