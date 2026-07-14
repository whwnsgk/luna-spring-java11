package com.example.riftarena.draft;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.example.riftarena.member.*;
import com.example.riftarena.rating.PositionCrowdingAdjuster;
import com.example.riftarena.balance.BalanceTuningService;

@RestController
@RequestMapping("/api/team-balance")
public class TeamController{
 private static final String[] LANES={"TOP","JUNGLE","MID","ADC","SUPPORT"};
 private static final List<int[]> PERMS=buildPermutations();
 private final MemberService members;
 private final BalanceTuningService tuning;

 public TeamController(MemberService members,BalanceTuningService tuning){this.members=members;this.tuning=tuning;}

 @SuppressWarnings("unchecked")
 @PostMapping("/auto")
 public Map<String,Object> auto(@RequestBody Map<String,Object> req){
  List<Number> raw=(List<Number>)req.get("memberIds");
  boolean forceYn=Boolean.TRUE.equals(req.get("forceYn"));
  if(raw==null||raw.size()!=10)throw new IllegalArgumentException("정확히 10명이 필요합니다.");
  List<Long> ids=new ArrayList<>();for(Number n:raw)ids.add(n.longValue());
  if(new HashSet<>(ids).size()!=10)throw new IllegalArgumentException("중복 멤버가 있습니다.");

  List<Map<String,Object>> original=members.byIds(ids);
  if(original.size()!=10)throw new IllegalArgumentException("멤버 조회에 실패했습니다.");
  List<Map<String,Object>> list=PositionCrowdingAdjuster.adjust(original);
  Map<String,Object> weights=tuning.setting();
  Set<Long> highestTierIds=highestTierMemberIds(original);

  List<Candidate> candidates=search(list,highestTierIds,weights,false,false);
  boolean emergency=false;
  boolean forced=false;
  if(candidates.isEmpty()){emergency=true;candidates=search(list,highestTierIds,weights,true,false);}
  if(candidates.isEmpty()&&forceYn){forced=true;candidates=search(list,highestTierIds,weights,true,true);}
  if(candidates.isEmpty())throw new IllegalArgumentException("현재 선택된 10명으로는 0점 라인 배치 없이 팀을 구성할 수 없습니다.");

  candidates.sort(Comparator.comparingDouble(c->c.score));
  List<Map<String,Object>> recommendations=new ArrayList<>();
  Set<String> seen=new HashSet<>();
  for(Candidate c:candidates){
   String key=signature(c);
   if(!seen.add(key))continue;
   recommendations.add(result(c,emergency));
   if(recommendations.size()==5)break;
  }

  Map<String,Object> response=new LinkedHashMap<>(recommendations.get(0));
  response.put("recommendations",recommendations);
  response.put("emergencySkill3Used",emergency);
  response.put("forcedTeam",forced);
  response.put("algorithmMessage",forced
   ?"강제 진행으로 0점 라인 또는 최고 티어 서포터 제한을 일부 해제했습니다."
   :emergency
    ?"2점·1점만으로 구성할 수 없어 3점 주라인을 비상 해제했습니다. 0점 배치는 사용하지 않았습니다."
    :"2점 라인을 우선하고 1점은 보조로 사용했습니다. 3점 주라인과 0점 라인은 사용하지 않았습니다.");
  return response;
 }

 @SuppressWarnings("unchecked")
 @PostMapping("/evaluate")
 public Map<String,Object> evaluate(@RequestBody Map<String,Object> req){
  List<Long> blueIds=longs((List<Number>)req.get("blueMemberIds"));
  List<Long> redIds=longs((List<Number>)req.get("redMemberIds"));
  if(blueIds.size()!=5||redIds.size()!=5)throw new IllegalArgumentException("각 팀은 5명이어야 합니다.");
  List<Map<String,Object>> original=new ArrayList<>();original.addAll(members.byIds(blueIds));original.addAll(members.byIds(redIds));
  List<Map<String,Object>> adjusted=PositionCrowdingAdjuster.adjust(original);
  Map<Long,Map<String,Object>> byId=new HashMap<>();for(Map<String,Object> m:adjusted)byId.put(id(m),m);
  List<Map<String,Object>> blue=new ArrayList<>(),red=new ArrayList<>();
  for(Long id:blueIds)blue.add(byId.get(id));for(Long id:redIds)red.add(byId.get(id));
  Set<Long> highest=highestTierMemberIds(original);Map<String,Object>w=tuning.setting();
  Candidate best=fixedTeamCandidate(blue,red,highest,w,false);
  if(best==null)best=fixedTeamCandidate(blue,red,highest,w,true);
  if(best==null)throw new IllegalArgumentException("0점 라인 배치 없이 현재 수동 팀의 라인을 구성할 수 없습니다.");
  return result(best,best.skill3Count>0);
 }

 private List<Candidate> search(List<Map<String,Object>> list,Set<Long> highest,Map<String,Object>w,boolean allow3,boolean force){
  /*
   * 강제 모드에서는 최대 126 × 120 × 120 = 1,814,400개 후보가 생길 수 있습니다.
   * 예전처럼 전부 List에 보관하면 무료 서버 메모리가 부족해질 수 있으므로,
   * 평가점수가 가장 좋은 후보 100개만 우선순위 큐에 유지합니다.
   */
  PriorityQueue<Candidate> best=new PriorityQueue<>(
   Comparator.comparingDouble((Candidate c)->c.score).reversed()
  );

  for(int mask=0;mask<(1<<10);mask++){
   if(Integer.bitCount(mask)!=5||(mask&1)==0)continue;
   List<Map<String,Object>> blue=new ArrayList<>(),red=new ArrayList<>();
   for(int i=0;i<10;i++)((mask&(1<<i))!=0?blue:red).add(list.get(i));

   List<LaneAssignment> ba=validAssignments(blue,highest,allow3,force);
   List<LaneAssignment> ra=validAssignments(red,highest,allow3,force);

   for(LaneAssignment b:ba){
    for(LaneAssignment r:ra){
     Candidate current=candidate(blue,red,b,r,w);
     if(best.size()<100)best.offer(current);
     else if(current.score<best.peek().score){
      best.poll();
      best.offer(current);
     }
    }
   }
  }

  return new ArrayList<>(best);
 }

 private Candidate fixedTeamCandidate(List<Map<String,Object>> blue,List<Map<String,Object>> red,Set<Long> highest,Map<String,Object>w,boolean allow3){
  Candidate best=null;
  for(LaneAssignment b:validAssignments(blue,highest,allow3,false))
   for(LaneAssignment r:validAssignments(red,highest,allow3,false)){
    Candidate c=candidate(blue,red,b,r,w);if(best==null||c.score<best.score)best=c;
   }
  return best;
 }

 private List<LaneAssignment> validAssignments(List<Map<String,Object>> team,Set<Long> highest,boolean allow3,boolean force){
  List<LaneAssignment> result=new ArrayList<>();
  for(int[] perm:PERMS){
   int s0=0,s1=0,s2=0,s3=0,topSupport=0;boolean valid=true;
   for(int i=0;i<5;i++){
    String lane=LANES[perm[i]];int skill=skill(team.get(i),lane);
    boolean highestSupport=lane.equals("SUPPORT")&&highest.contains(id(team.get(i)));
    if(!force&&(skill==0||(!allow3&&skill==3)||highestSupport)){valid=false;break;}
    if(skill==0)s0++;else if(skill==1)s1++;else if(skill==2)s2++;else if(skill==3)s3++;
    if(highestSupport)topSupport++;
   }
   if(valid)result.add(new LaneAssignment(perm,s0,s1,s2,s3,topSupport));
  }
  return result;
 }

 private Candidate candidate(List<Map<String,Object>> blue,List<Map<String,Object>> red,LaneAssignment b,LaneAssignment r,Map<String,Object>w){
  int bc=cost(blue),rc=cost(red),teamDiff=Math.abs(bc-rc),matchupDiff=0;
  for(int lane=0;lane<5;lane++){
   Map<String,Object> blueMember=blue.get(b.memberByLane[lane]);
   Map<String,Object> redMember=red.get(r.memberByLane[lane]);
   matchupDiff+=Math.abs(score(blueMember)-score(redMember));
  }

  int s0=b.skill0+r.skill0,s1=b.skill1+r.skill1,s2=b.skill2+r.skill2,s3=b.skill3+r.skill3,topSupport=b.topSupport+r.topSupport;
  int crowd=0;
  for(Map<String,Object>m:blue)if(number(m.get("positionCrowdingPenalty"))>0)crowd++;
  for(Map<String,Object>m:red)if(number(m.get("positionCrowdingPenalty"))>0)crowd++;

  double total=matchupDiff*num(w,"matchupDiffWeight",1)+teamDiff*num(w,"teamDiffWeight",.35)
   +s1*num(w,"skill1Penalty",120)+s3*num(w,"skill3Penalty",300)+crowd*num(w,"crowdingPenalty",50)
   +s0*5000.0+topSupport*num(w,"topTierSupportPenalty",1000000);

  /*
   * 여기서는 배정 결과 Map을 만들지 않습니다.
   * 원본 팀과 작은 int 배열만 보관하고, 최종 상위 후보에 들어온 경우에만 result()에서 Map을 생성합니다.
   */
  return new Candidate(blue,red,b,r,bc,rc,matchupDiff,teamDiff,s0,s1,s2,s3,crowd,topSupport,total);
 }

 private Map<String,Object> result(Candidate c,boolean emergency){
  List<Map<String,Object>> blueAssigned=assigned(c.blue,c.blueAssignment);
  List<Map<String,Object>> redAssigned=assigned(c.red,c.redAssignment);
  double bp=1/(1+Math.pow(10,(c.redCost-c.blueCost)/400.0));

  Map<String,Object>m=new LinkedHashMap<>();
  m.put("blueTeam",blueAssigned);m.put("redTeam",redAssigned);m.put("blueCost",c.blueCost);m.put("redCost",c.redCost);
  m.put("costDifference",c.teamDiff);m.put("matchupDifference",c.matchupDiff);
  m.put("blueExpectedWinRate",Math.round(bp*1000)/10.0);m.put("redExpectedWinRate",Math.round((1-bp)*1000)/10.0);
  m.put("skill0Count",c.skill0Count);m.put("skill1Count",c.skill1Count);m.put("skill2Count",c.skill2Count);m.put("skill3Count",c.skill3Count);
  m.put("crowdingCount",c.crowdingCount);m.put("topTierSupportCount",c.topTierSupportCount);
  m.put("algorithmScore",Math.round(c.score*10)/10.0);m.put("emergencySkill3Used",emergency||c.skill3Count>0);

  Map<String,Object>feature=new LinkedHashMap<>();
  feature.put("matchupDiff",c.matchupDiff);feature.put("teamDiff",c.teamDiff);
  feature.put("skill1Count",c.skill1Count);feature.put("skill2Count",c.skill2Count);feature.put("skill3Count",c.skill3Count);
  feature.put("crowdingCount",c.crowdingCount);feature.put("topTierSupportCount",c.topTierSupportCount);feature.put("algorithmScore",c.score);
  m.put("balanceFeature",feature);
  m.put("algorithmMessage","맞라인 차이 "+c.matchupDiff+" · 팀 총점 차이 "+c.teamDiff+" · 2점 "+c.skill2Count+"명 · 1점 "+c.skill1Count+"명 · 3점 "+c.skill3Count+"명 · 0점 "+c.skill0Count+"명");
  return m;
 }

 private List<Map<String,Object>> assigned(List<Map<String,Object>> team,LaneAssignment a){
  List<Map<String,Object>> result=new ArrayList<>();
  for(int i=0;i<5;i++){Map<String,Object>m=new LinkedHashMap<>(team.get(i));m.put("assignedPosition",LANES[a.perm[i]]);m.put("assignedSkill",skill(m,LANES[a.perm[i]]));result.add(m);}
  result.sort(Comparator.comparingInt(m->laneOrder(String.valueOf(m.get("assignedPosition")))));return result;
 }

 @SuppressWarnings("unchecked") private int skill(Map<String,Object>m,String lane){
  Object raw=m.get("laneProfiles");if(!(raw instanceof List))return 0;
  for(Object o:(List<Object>)raw)if(o instanceof Map){Map<String,Object>p=(Map<String,Object>)o;if(lane.equals(String.valueOf(p.get("positionCode"))))return number(p.get("preferenceScore"));}
  return 0;
 }
 private Set<Long> highestTierMemberIds(List<Map<String,Object>> list){
  int max=Integer.MIN_VALUE;Map<Long,Integer>scores=new HashMap<>();
  for(Map<String,Object>m:list){int v=tierScore(m);scores.put(id(m),v);max=Math.max(max,v);}
  Set<Long>r=new HashSet<>();for(Map.Entry<Long,Integer>e:scores.entrySet())if(e.getValue()==max)r.add(e.getKey());return r;
 }
 private int tierScore(Map<String,Object>m){
  String tier=String.valueOf(m.getOrDefault("soloTier","UNRANKED")).toUpperCase(Locale.ROOT);
  Map<String,Integer>b=new HashMap<>();b.put("IRON",0);b.put("BRONZE",50);b.put("SILVER",100);b.put("GOLD",200);b.put("PLATINUM",300);b.put("EMERALD",400);b.put("DIAMOND",500);b.put("MASTER",600);b.put("GRANDMASTER",700);b.put("CHALLENGER",800);b.put("UNRANKED",100);
  Map<String,Integer>r=new HashMap<>();r.put("IV",0);r.put("III",25);r.put("II",50);r.put("I",75);
  return b.getOrDefault(tier,100)+r.getOrDefault(String.valueOf(m.get("soloRank")),0);
 }
 private int cost(List<Map<String,Object>>t){int s=0;for(Map<String,Object>m:t)s+=score(m);return s;}
 private int score(Map<String,Object>m){return number(m.get("balanceScore"));}
 private long id(Map<String,Object>m){return ((Number)m.get("memberId")).longValue();}
 private int number(Object o){return o instanceof Number?((Number)o).intValue():0;}
 private double num(Map<String,Object>m,String k,double d){Object o=m.get(k);return o instanceof Number?((Number)o).doubleValue():d;}
 private List<Long> longs(List<Number>x){List<Long>r=new ArrayList<>();if(x!=null)for(Number n:x)r.add(n.longValue());return r;}
 private String signature(Candidate c){
  StringBuilder s=new StringBuilder();
  for(int lane=0;lane<5;lane++)s.append(id(c.blue.get(c.blueAssignment.memberByLane[lane]))).append(':').append(LANES[lane]).append('|');
  s.append('/');
  for(int lane=0;lane<5;lane++)s.append(id(c.red.get(c.redAssignment.memberByLane[lane]))).append(':').append(LANES[lane]).append('|');
  return s.toString();
 }
 private static int laneOrder(String l){for(int i=0;i<LANES.length;i++)if(LANES[i].equals(l))return i;return 9;}
 private static List<int[]> buildPermutations(){List<int[]>r=new ArrayList<>();perm(new int[]{0,1,2,3,4},0,r);return r;}
 private static void perm(int[]v,int x,List<int[]>r){if(x==v.length){r.add(Arrays.copyOf(v,v.length));return;}for(int i=x;i<v.length;i++){int t=v[x];v[x]=v[i];v[i]=t;perm(v,x+1,r);t=v[x];v[x]=v[i];v[i]=t;}}

 static class LaneAssignment{
  int[]perm,memberByLane;
  int skill0,skill1,skill2,skill3,topSupport;
  LaneAssignment(int[]p,int s0,int s1,int s2,int s3,int ts){
   perm=Arrays.copyOf(p,p.length);
   memberByLane=new int[5];
   for(int memberIndex=0;memberIndex<perm.length;memberIndex++)memberByLane[perm[memberIndex]]=memberIndex;
   skill0=s0;skill1=s1;skill2=s2;skill3=s3;topSupport=ts;
  }
 }
 static class Candidate{
  List<Map<String,Object>>blue,red;
  LaneAssignment blueAssignment,redAssignment;
  int blueCost,redCost,matchupDiff,teamDiff,skill0Count,skill1Count,skill2Count,skill3Count,crowdingCount,topTierSupportCount;
  double score;
  Candidate(List<Map<String,Object>>b,List<Map<String,Object>>r,LaneAssignment ba,LaneAssignment ra,int bc,int rc,int md,int td,int s0,int s1,int s2,int s3,int crowd,int ts,double sc){
   blue=b;red=r;blueAssignment=ba;redAssignment=ra;blueCost=bc;redCost=rc;matchupDiff=md;teamDiff=td;
   skill0Count=s0;skill1Count=s1;skill2Count=s2;skill3Count=s3;crowdingCount=crowd;topTierSupportCount=ts;score=sc;
  }
 }
}
