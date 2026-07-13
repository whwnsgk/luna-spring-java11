package com.example.riftarena.draft;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import com.example.riftarena.member.*;
@RestController
@RequestMapping("/api/team-balance")
public class TeamController {
 private final MemberService members;
 public TeamController(MemberService members){this.members=members;}
 @PostMapping("/auto") public Map<String,Object> auto(@RequestBody Map<String,Object> req){
  List<Integer> raw=(List<Integer>)req.get("memberIds"); if(raw==null||raw.size()!=10)throw new IllegalArgumentException("정확히 10명이 필요합니다.");
  List<Long> ids=new ArrayList<>();for(Number n:raw)ids.add(n.longValue());
  if(new HashSet<>(ids).size()!=10)throw new IllegalArgumentException("중복 멤버가 있습니다.");
  List<Map<String,Object>> list=members.byIds(ids); if(list.size()!=10)throw new IllegalArgumentException("멤버 조회에 실패했습니다.");
  Candidate best=null;
  for(int mask=0;mask<(1<<10);mask++){if(Integer.bitCount(mask)!=5||(mask&1)==0)continue;
   List<Map<String,Object>> b=new ArrayList<>(),r=new ArrayList<>();for(int i=0;i<10;i++)((mask&(1<<i))!=0?b:r).add(list.get(i));
   int bc=cost(b),rc=cost(r),pen=Math.abs(bc-rc)+80*(posPenalty(b)+posPenalty(r));
   if(best==null||pen<best.pen)best=new Candidate(b,r,bc,rc,pen);
  }
  return result(best.b,best.r,best.bc,best.rc,"코스트와 포지션 커버리지를 함께 고려했습니다.");
 }
 @PostMapping("/evaluate") public Map<String,Object> eval(@RequestBody Map<String,Object> req){
  List<Long>b=longs((List<Integer>)req.get("blueMemberIds")),r=longs((List<Integer>)req.get("redMemberIds"));
  if(b.size()!=5||r.size()!=5)throw new IllegalArgumentException("각 팀은 5명이어야 합니다.");
  List<Map<String,Object>> bm=members.byIds(b),rm=members.byIds(r);return result(bm,rm,cost(bm),cost(rm),"현재 수동 편성 기준입니다.");
 }
 private List<Long> longs(List<Integer>x){List<Long>r=new ArrayList<>();if(x!=null)for(Number n:x)r.add(n.longValue());return r;}
 private int cost(List<Map<String,Object>>t){int s=0;for(Map<String,Object>m:t){int v=((Number)m.getOrDefault("balanceScore",1000)).intValue();Number mc=(Number)m.get("matchCount"),wr=(Number)m.get("inhouseWinRate");if(mc!=null&&mc.intValue()>=3&&wr!=null)v+=(int)Math.round((wr.doubleValue()-50)*3);s+=v;}return s;}
 private int posPenalty(List<Map<String,Object>>t){String[]p={"TOP","JUNGLE","MID","ADC","SUPPORT"};int miss=0;for(String x:p){boolean ok=false;for(Map<String,Object>m:t){List<String>ps=(List<String>)m.get("preferredPositions");if(ps!=null&&(ps.contains(x)||ps.contains("FILL"))){ok=true;break;}}if(!ok)miss++;}return miss;}
 private Map<String,Object> result(List<Map<String,Object>>b,List<Map<String,Object>>r,int bc,int rc,String msg){double bp=1/(1+Math.pow(10,(rc-bc)/800.0));Map<String,Object>m=new LinkedHashMap<>();m.put("blueTeam",b);m.put("redTeam",r);m.put("blueCost",bc);m.put("redCost",rc);m.put("costDifference",Math.abs(bc-rc));m.put("blueExpectedWinRate",Math.round(bp*1000)/10.0);m.put("redExpectedWinRate",Math.round((1-bp)*1000)/10.0);m.put("algorithmMessage",msg);return m;}
 static class Candidate{List<Map<String,Object>>b,r;int bc,rc,pen;Candidate(List<Map<String,Object>>b,List<Map<String,Object>>r,int bc,int rc,int pen){this.b=b;this.r=r;this.bc=bc;this.rc=rc;this.pen=pen;}}
}
